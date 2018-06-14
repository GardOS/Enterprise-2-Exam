package no.exam.sale.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.exam.sale.model.Sale
import no.exam.sale.model.SaleConverter
import no.exam.sale.model.SaleRepository
import no.exam.schema.BookDto
import no.exam.schema.SaleDto
import no.exam.schema.SellerDto
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.TransactionSystemException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.security.Principal
import javax.servlet.http.HttpServletResponse
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import javax.validation.ConstraintViolationException as JavaxConstraintViolationException

@Api(value = "/sales", description = "API for Sale")
@RequestMapping(
		path = ["/sales"],
		produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
@RestController
@Validated
class SaleController {
	@Autowired
	private lateinit var restTemplate: RestTemplate

	@Autowired
	private lateinit var saleRepo: SaleRepository

	@Autowired
	private lateinit var rabbitTemplate: RabbitTemplate

	@Autowired
	private lateinit var saleCreatedFanout: FanoutExchange

	@Autowired
	private lateinit var saleUpdatedFanout: FanoutExchange

	@Autowired
	private lateinit var saleDeletedFanout: FanoutExchange

	@Value("\${bookServerPath}")
	private lateinit var bookServerPath: String

	@Value("\${sellerServerPath}")
	private lateinit var sellerServerPath: String

	@ApiOperation("Get all sales")
	@GetMapping
	fun getAllSales(): ResponseEntity<List<SaleDto>> {
		return ResponseEntity.ok(SaleConverter.transform(saleRepo.findAll()))
	}

	@ApiOperation("Get sale by id")
	@GetMapping(path = ["/{id}"])
	fun getSale(
			@ApiParam("Id of sale")
			@PathVariable("id")
			pathId: Long
	): ResponseEntity<Any> {
		val sale = saleRepo.findOne(pathId)
				?: return ResponseEntity.status(404).body("Sale with id: $pathId not found")

		return ResponseEntity.ok(SaleConverter.transform(sale))
	}

	@ApiOperation("Get all books that are for sale")
	@GetMapping(path = ["/books"])
	fun getAllBooksOnSale(): ResponseEntity<Any> {
		//TODO: New book endpoint
		return ResponseEntity.status(204).build()
	}

	@ApiOperation("Get sales on a specific book")
	@GetMapping(path = ["/books/{id}"])
	fun getSalesForBook(
			@ApiParam("Id of book")
			@PathVariable("id")
			pathId: Long
	): ResponseEntity<Any> {
		val sales = saleRepo.findByBook(pathId)

		if (sales.isEmpty())
			ResponseEntity.status(204)

		return ResponseEntity.ok(SaleConverter.transform(sales))
	}

	@ApiOperation("Get all sellers that are currently selling books")
	@GetMapping(path = ["/sellers"])
	fun getAllSellersSellingBooks(): ResponseEntity<Any> {
		//TODO: New seller endpoint
		return ResponseEntity.status(204).build()
	}

	@ApiOperation("Get sales from a specific seller")
	@GetMapping(path = ["/sellers/{username}"])
	fun getSalesForSeller(
			@ApiParam("Username of seller")
			@PathVariable("username")
			pathId: String
	): ResponseEntity<Any> {
		val sales = saleRepo.findBySeller(pathId)

		if (sales.isEmpty())
			return ResponseEntity.status(204).build()

		return ResponseEntity.ok(SaleConverter.transform(sales))
	}

	@ApiOperation("Get a specific book from a specific seller")
	@GetMapping(path = ["/sellers/{username}/books/{bookId}"])
	fun getBookFromSeller(
			@ApiParam("Username of seller")
			@PathVariable("username")
			username: String,
			@ApiParam("Id of book")
			@PathVariable("bookId")
			bookId: Long
	): ResponseEntity<Any> {
		val sales = saleRepo.findBySeller(username)

		for (sale in sales){
			if(sale.book == bookId){
				return ResponseEntity.status(200).body(SaleConverter.transform(sale))
			}
		}
		return ResponseEntity.status(404).build()
	}

	//POST
	@ApiOperation("Create new sale")
	@PostMapping(consumes = [(MediaType.APPLICATION_JSON_VALUE)])
	fun createSale(
			@ApiParam("Sale dto. Should not specify id, nor seller")
			@RequestBody
			dto: SaleDto,
			principal: Principal
	): ResponseEntity<Any> {
		//Id is auto-generated and should not be specified
		if (dto.id != null)
			return ResponseEntity.status(400).body("Id should not be specified")

		if (dto.seller != null)
			return ResponseEntity.status(400).body("Seller should not be specified")


		//Find book
		val book: BookDto
		try {
			book = restTemplate.getForObject("$bookServerPath/${dto.book}", BookDto::class.java)
		} catch (ex: HttpClientErrorException) {
			return ResponseEntity.status(ex.statusCode).body("Error when querying for Book:\n" +
					"$ex.responseBodyAsString")
		}

		//Find seller
		val seller: SellerDto
		try {
			seller = restTemplate.getForObject("$sellerServerPath/${principal.name}", SellerDto::class.java)
		} catch (ex: HttpClientErrorException) {
			return ResponseEntity.status(ex.statusCode).body("Error when querying for Book:\n" +
					"$ex.responseBodyAsString")
		}

		val sale = saleRepo.save(
				Sale(
						seller = seller.username,
						book = book.id,
						price = dto.price,
						condition = dto.condition
				)
		)

		//RabbitMQ news
		rabbitTemplate.convertAndSend(saleCreatedFanout.name, "", SaleConverter.transform(sale))

		return ResponseEntity.status(201).build()
	}

	//PATCH
	@ApiOperation("Update price and/or condition for the book being sold")
	@PatchMapping(path = ["/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun updateSale(
			@ApiParam("Id of sale")
			@PathVariable("id")
			pathId: Long,
			@ApiParam("Updated sale information. Id, seller and book should not be included")
			@RequestBody
			dto: SaleDto,
			principal: Principal
	): ResponseEntity<Any> {
		if (!saleRepo.exists(pathId))
			return ResponseEntity.status(404).body("Sale with id: $pathId not found")

		if (dto.id != null || dto.seller != null || dto.book != null)
			return ResponseEntity.status(400).body("Field(s) not eligible for update")

		val sale = saleRepo.findOne(pathId)

		if (sale.seller != principal.name)
			return ResponseEntity.status(403).body("Mismatch between current seller and owner of sale")

		if (dto.price == sale.price && dto.condition == sale.condition)
			return ResponseEntity.status(400).body("No change found")

		if (dto.price != null)
			sale.price = dto.price

		if (dto.condition != null)
			sale.condition = dto.condition

		saleRepo.save(sale)

		rabbitTemplate.convertAndSend(saleUpdatedFanout.name, "", SaleConverter.transform(sale))

		return ResponseEntity.status(204).build()
	}

	//DELETE
	@ApiOperation("Delete sale")
	@DeleteMapping(path = ["/{id}"])
	fun deleteSale(
			@ApiParam("Id of sale")
			@PathVariable("id")
			pathId: Long,
			principal: Principal
	): ResponseEntity<Any> {
		if (!saleRepo.exists(pathId))
			return ResponseEntity.status(404).body("Sale with id: $pathId not found")

		val sale = saleRepo.findOne(pathId)

		if (sale.seller != principal.name)
			return ResponseEntity.status(403).body("Mismatch between current seller and owner of sale")

		saleRepo.delete(sale)

		rabbitTemplate.convertAndSend(saleDeletedFanout.name, "", SaleConverter.transform(sale))

		return ResponseEntity.status(204).build()
	}

	//Catches validation errors and returns error status based on error
	@ExceptionHandler(value = ([JavaxConstraintViolationException::class, HibernateConstraintViolationException::class,
		DataIntegrityViolationException::class, TransactionSystemException::class]))
	fun handleValidationFailure(ex: Exception, response: HttpServletResponse): String {
		var cause: Throwable? = ex
		for (i in 0..4) { //Iterate 5 times max, since it might have infinite depth
			if (cause is JavaxConstraintViolationException || cause is HibernateConstraintViolationException) {
				response.status = HttpStatus.BAD_REQUEST.value()
				return "Invalid request. Error:\n${ex.message ?: "Error not found"}"
			}
			cause = cause?.cause
		}
		response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
		return "Something went wrong processing the request.  Error:\n${ex.message ?: "Error not found"}"
	}
}