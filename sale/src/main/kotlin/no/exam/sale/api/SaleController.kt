package no.exam.sale.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.exam.sale.model.Sale
import no.exam.sale.model.SaleConverter
import no.exam.sale.model.SaleRepository
import no.exam.schema.ProducerDto
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
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import javax.validation.ConstraintViolationException as JavaxConstraintViolationException

@Api(value = "/sale", description = "API for sale")
@RequestMapping(
		path = ["/sale"],
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

	//RABBIT
	@ApiOperation("Send message via rabbitmq")
	@PostMapping(path = ["/rabbit"])
	fun rabbitmq(
			@ApiParam("Sale dto. Should not specify id")
			@RequestBody
			dto: SaleDto
	): ResponseEntity<Any> {
		val sale = SaleDto()
		rabbitTemplate.convertAndSend(fanout.name, "", sale)
		return ResponseEntity.ok().build()
	}

	//REST_TEMPLATE
	@ApiOperation("Send message with RestTemplate")
	@PostMapping(path = ["/rest-template"], consumes = [(MediaType.APPLICATION_JSON_VALUE)])
	fun sendMessage(
			@ApiParam("Sale dto. Should not specify id")
			@RequestBody
			dto: ProducerDto,
			session: HttpSession
	): ResponseEntity<Any> {
		//Id is auto-generated and should not be specified
		if (dto.id != null) {
			return ResponseEntity.status(400).body("Id should not be specified")
		}

		val url = bookServerPath
		val headers = HttpHeaders()
		headers.add("cookie", "SESSION=${session.id}")
		val httpEntity = HttpEntity(ProducerDto(name = dto.name), headers)

		val status: HttpStatus
		try {
			status = restTemplate.exchange(url, HttpMethod.POST, httpEntity, ProducerDto::class.java).statusCode
		} catch (ex: HttpClientErrorException) {
			return ResponseEntity.status(ex.statusCode).body("Error when querying Producer:\n" +
					"$ex.responseBodyAsString")
		}

		return ResponseEntity.status(status).build()
	}

	//GET ALL
	@ApiOperation("Get all sales")
	@GetMapping
	fun getAllSales(): ResponseEntity<List<SaleDto>> {
		return ResponseEntity.ok(SaleConverter.transform(saleRepo.findAll()))
	}

	//GET ONE
	@ApiOperation("Get sale by id")
	@GetMapping(path = ["/{id}"])
	fun getSale(
			@ApiParam("Id of sale")
			@PathVariable("id")
			pathId: Long
	): ResponseEntity<Any> {
		val sale = saleRepo.findOne(pathId)
				?: return ResponseEntity.status(404).body("sale with id: $pathId not found")

		return ResponseEntity.ok(SaleConverter.transform(sale))
	}

	//POST
	@ApiOperation("Create new sale")
	@PostMapping(consumes = [(MediaType.APPLICATION_JSON_VALUE)])
	fun createSale(
			@ApiParam("Sale dto. Should not specify id")
			@RequestBody
			dto: SaleDto
	): ResponseEntity<Any> {
		//Id is auto-generated and should not be specified
		if (dto.id != null) {
			return ResponseEntity.status(400).body("Id should not be specified")
		}

		saleRepo.save(Sale())

		return ResponseEntity.status(201).build()
	}

	//PATCH
	@ApiOperation("Update name of sale")
	@PatchMapping(path = ["/{id}"], consumes = [MediaType.TEXT_PLAIN_VALUE])
	fun updateSaleName(
			@ApiParam("Id of sale")
			@PathVariable("id")
			pathId: Long,
			@ApiParam("The new name for the sale")
			@RequestBody
			name: String
	): ResponseEntity<Any> {
		if (!saleRepo.exists(pathId))
			return ResponseEntity.status(404).body("sale with id: $pathId not found")

		val newSale = saleRepo.save(Sale())

		return ResponseEntity.ok(SaleConverter.transform(newSale))
	}

	//PUT
	@ApiOperation("Update an existing sale")
	@PutMapping(path = ["/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun updateSale(
			@ApiParam("Id of sale")
			@PathVariable("id")
			pathId: Long,
			@ApiParam("The new sale which will replace the old one")
			@RequestBody
			requestDto: SaleDto
	): ResponseEntity<Any> {
		if (requestDto.id != null) {
			return ResponseEntity.status(400).body("Id should not be specified")
		}

		if (!saleRepo.exists(pathId))
			return ResponseEntity.status(404).body("Sale with id: $pathId not found")

		val newSale = saleRepo.save(Sale())

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