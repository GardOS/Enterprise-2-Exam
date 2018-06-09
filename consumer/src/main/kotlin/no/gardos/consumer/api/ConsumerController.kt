package no.gardos.consumer.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.gardos.consumer.model.Consumer
import no.gardos.consumer.model.ConsumerConverter
import no.gardos.consumer.model.ConsumerRepository
import no.gardos.schema.ConsumerDto
import no.gardos.schema.ProducerDto
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

@Api(value = "/consumer", description = "API for consumer")
@RequestMapping(
		path = ["/consumer"],
		produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
@RestController
@Validated
class ConsumerController {
	@Autowired
	private lateinit var restTemplate: RestTemplate

	@Autowired
	private lateinit var consumerRepo: ConsumerRepository

	@Autowired
	private lateinit var rabbitTemplate: RabbitTemplate

	@Autowired
	private lateinit var fanout: FanoutExchange

	@Value("\${producerServerPath}")
	private lateinit var producerServerPath: String

	//RABBIT
	@ApiOperation("Send message via rabbitmq")
	@PostMapping(path = ["/rabbit"])
	fun rabbitmq(
			@ApiParam("Consumer dto. Should not specify id")
			@RequestBody
			dto: ConsumerDto
	): ResponseEntity<Any> {
		val consumer = ConsumerDto(id = 123, name = dto.name)
		rabbitTemplate.convertAndSend(fanout.name, "", consumer)
		return ResponseEntity.ok().build()
	}

	//REST_TEMPLATE
	@ApiOperation("Send message with RestTemplate")
	@PostMapping(path = ["/rest-template"], consumes = [(MediaType.APPLICATION_JSON_VALUE)])
	fun sendMessage(
			@ApiParam("Consumer dto. Should not specify id")
			@RequestBody
			dto: ProducerDto,
			session: HttpSession
	): ResponseEntity<Any> {
		//Id is auto-generated and should not be specified
		if (dto.id != null) {
			return ResponseEntity.status(400).body("Id should not be specified")
		}

		val url = producerServerPath
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
	@ApiOperation("Get all consumers")
	@GetMapping
	fun getAllConsumers(): ResponseEntity<List<ConsumerDto>> {
		return ResponseEntity.ok(ConsumerConverter.transform(consumerRepo.findAll()))
	}

	//GET ONE
	@ApiOperation("Get consumer by id")
	@GetMapping(path = ["/{id}"])
	fun getConsumer(
			@ApiParam("Id of consumer")
			@PathVariable("id")
			pathId: Long
	): ResponseEntity<Any> {
		val consumer = consumerRepo.findOne(pathId)
				?: return ResponseEntity.status(404).body("consumer with id: $pathId not found")

		return ResponseEntity.ok(ConsumerConverter.transform(consumer))
	}

	//POST
	@ApiOperation("Create new consumer")
	@PostMapping(consumes = [(MediaType.APPLICATION_JSON_VALUE)])
	fun createConsumer(
			@ApiParam("Consumer dto. Should not specify id")
			@RequestBody
			dto: ConsumerDto
	): ResponseEntity<Any> {
		//Id is auto-generated and should not be specified
		if (dto.id != null) {
			return ResponseEntity.status(400).body("Id should not be specified")
		}

		consumerRepo.save(Consumer(name = dto.name))

		return ResponseEntity.status(201).build()
	}

	//PATCH
	@ApiOperation("Update name of consumer")
	@PatchMapping(path = ["/{id}"], consumes = [MediaType.TEXT_PLAIN_VALUE])
	fun updateConsumerName(
			@ApiParam("Id of consumer")
			@PathVariable("id")
			pathId: Long,
			@ApiParam("The new name for the consumer")
			@RequestBody
			name: String
	): ResponseEntity<Any> {
		if (!consumerRepo.exists(pathId))
			return ResponseEntity.status(404).body("consumer with id: $pathId not found")

		val newConsumer = consumerRepo.save(Consumer(name = name))

		return ResponseEntity.ok(ConsumerConverter.transform(newConsumer))
	}

	//PUT
	@ApiOperation("Update an existing consumer")
	@PutMapping(path = ["/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun updateConsumer(
			@ApiParam("Id of consumer")
			@PathVariable("id")
			pathId: Long,
			@ApiParam("The new consumer which will replace the old one")
			@RequestBody
			requestDto: ConsumerDto
	): ResponseEntity<Any> {
		if (requestDto.id != null) {
			return ResponseEntity.status(400).body("Id should not be specified")
		}

		if (!consumerRepo.exists(pathId))
			return ResponseEntity.status(404).body("Consumer with id: $pathId not found")

		val newConsumer = consumerRepo.save(Consumer(name = requestDto.name))

		return ResponseEntity.ok(ConsumerConverter.transform(newConsumer))
	}

	//DELETE
	@ApiOperation("Delete consumer")
	@DeleteMapping(path = ["/{id}"])
	fun deleteConsumer(
			@ApiParam("Id of consumer")
			@PathVariable("id")
			pathId: Long
	): ResponseEntity<Any> {
		if (!consumerRepo.exists(pathId))
			return ResponseEntity.status(404).body("Consumer with id: $pathId not found")

		consumerRepo.delete(pathId)

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