package no.exam.sale.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.exam.sale.model.Sale
import no.exam.sale.model.SaleConverter
import no.exam.sale.model.SaleRepository
import no.exam.schema.BookDto
import no.exam.schema.NewsDto
import no.exam.schema.SaleDto
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.*
import org.springframework.transaction.TransactionSystemException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.security.Principal
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
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
	private lateinit var fanout: FanoutExchange

	@Value("\${bookServerPath}")
	private lateinit var bookServerPath: String

	@Value("\${userServerPath}")
	private lateinit var userServerPath: String

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

	@ApiOperation("Get all users that are currently selling books")
	@GetMapping(path = ["/users"])
	fun getAllUsersSellingBooks(): ResponseEntity<Any> {
		//TODO: New user endpoint
		return ResponseEntity.status(204).build()
	}

	@ApiOperation("Get sales from a specific user")
	@GetMapping(path = ["/users/{username}"])
	fun getSalesForUser(
			@ApiParam("Username of user")
			@PathVariable("username")
			pathId: String
	): ResponseEntity<Any> {
		val users = saleRepo.findByUser(pathId)

		if (users.isEmpty())
			ResponseEntity.status(204)

		return ResponseEntity.ok(SaleConverter.transform(users))
	}

	//POST
	@ApiOperation("Create new sale")
	@PostMapping(consumes = [(MediaType.APPLICATION_JSON_VALUE)])
	fun createSale(
			@ApiParam("Sale dto. Should not specify id, nor user")
			@RequestBody
			dto: SaleDto,
			principal: Principal
	): ResponseEntity<Any> {
		//Id is auto-generated and should not be specified
		if (dto.id != null)
			return ResponseEntity.status(400).body("Id should not be specified")

		if (dto.user != null)
			return ResponseEntity.status(400).body("User should not be specified")


		//Find book
		var book: BookDto
		try {
			book = restTemplate.getForObject("$bookServerPath/${dto.book}", BookDto::class.java)
		} catch (ex: HttpClientErrorException) {
			return ResponseEntity.status(ex.statusCode).body("Error when querying for Book:\n" +
					"$ex.responseBodyAsString")
		}

		//Find user
		//TODO: Find user using user service

		val sale = saleRepo.save(
				Sale(
						user = principal.name,
						book = book.id)
		)

		val newsDto = NewsDto(
				sale = sale.id,
				sellerName = sale.user,
				bookTitle = book.title,
				bookPrice = sale.price
		)

		//RabbitMQ news
		rabbitTemplate.convertAndSend(fanout.name, "", newsDto)

		return ResponseEntity.status(201).build()
	}

	//PATCH
	@ApiOperation("Update price of sale")
	@PatchMapping(path = ["/{id}"], consumes = [MediaType.TEXT_PLAIN_VALUE])
	fun updatePrice(
			@ApiParam("Id of sale")
			@PathVariable("id")
			pathId: Long,
			@ApiParam("The new price for the sale")
			@RequestBody
			price: Int
	): ResponseEntity<Any> {
		if (!saleRepo.exists(pathId))
			return ResponseEntity.status(404).body("Sale with id: $pathId not found")

		val newSale = saleRepo.save(Sale())

		//TODO: Post update

		return ResponseEntity.ok(SaleConverter.transform(newSale))
	}

	//DELETE
	@ApiOperation("Delete sale")
	@DeleteMapping(path = ["/{id}"])
	fun deleteSale(
			@ApiParam("Id of sale")
			@PathVariable("id")
			pathId: Long
	): ResponseEntity<Any> {
		if (!saleRepo.exists(pathId))
			return ResponseEntity.status(404).body("Sale with id: $pathId not found")

		saleRepo.delete(pathId)

		//TODO: Post update

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

	//RABBIT
	@ApiOperation("Send message via rabbitmq")
	@PostMapping(path = ["/rabbit"])
	fun rabbitmq(
			@ApiParam("Sale dto. Should not specify id")
			@RequestBody
			dto: SaleDto
	): ResponseEntity<Any> {
		val sale = SaleDto(
				id = 123,
				user = "User",
				book = 123,
				price = 1234
		)
		rabbitTemplate.convertAndSend(fanout.name, "", sale)
		return ResponseEntity.ok().build()
	}

	//REST_TEMPLATE
	@ApiOperation("Send message with RestTemplate")
	@PostMapping(path = ["/rest-template"], consumes = [(MediaType.APPLICATION_JSON_VALUE)])
	fun sendMessage(
			@ApiParam("Sale dto. Should not specify id")
			@RequestBody
			dto: BookDto,
			session: HttpSession
	): ResponseEntity<Any> {
		//Id is auto-generated and should not be specified
		if (dto.id != null) {
			return ResponseEntity.status(400).body("Id should not be specified")
		}

		val url = bookServerPath
		val headers = HttpHeaders()
		headers.add("cookie", "SESSION=${session.id}")
		val httpEntity = HttpEntity(BookDto(), headers)

		val status: HttpStatus
		try {
			status = restTemplate.exchange(url, HttpMethod.POST, httpEntity, BookDto::class.java).statusCode
		} catch (ex: HttpClientErrorException) {
			return ResponseEntity.status(ex.statusCode).body("Error when querying Producer:\n" +
					"$ex.responseBodyAsString")
		}

		return ResponseEntity.status(status).build()
	}
}