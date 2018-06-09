package no.gardos.producer.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.gardos.producer.model.Producer
import no.gardos.producer.model.ProducerConverter
import no.gardos.producer.model.ProducerRepository
import no.gardos.schema.ConsumerDto
import no.gardos.schema.ProducerDto
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.TransactionSystemException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import javax.validation.ConstraintViolationException as JavaxConstraintViolationException

@Api(value = "/producer", description = "API for producer")
@RequestMapping(
		path = ["/producer"],
		produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
@RestController
@Validated
class ProducerController {
	@Autowired
	private lateinit var producerRepo: ProducerRepository

	//RABBIT
	@RabbitListener(queues = ["#{queue.name}"])
	fun rabbitMq(consumer: ConsumerDto) {
		try {
			producerRepo.save(Producer(name = consumer.name))
		} catch (e: Exception) {
		}
	}

	//GET ALL
	@ApiOperation("Get all producers")
	@GetMapping
	fun getAllProducers(): ResponseEntity<List<ProducerDto>> {
		return ResponseEntity.ok(ProducerConverter.transform(producerRepo.findAll()))
	}

	//GET ONE
	@ApiOperation("Get producer by id")
	@GetMapping(path = ["/{id}"])
	fun getProducer(
			@ApiParam("Id of producer")
			@PathVariable("id")
			pathId: Long
	): ResponseEntity<Any> {
		val producer = producerRepo.findOne(pathId)
				?: return ResponseEntity.status(404).body("producer with id: $pathId not found")

		return ResponseEntity.ok(ProducerConverter.transform(producer))
	}

	//POST
	@ApiOperation("Create new producer")
	@PostMapping(consumes = [(MediaType.APPLICATION_JSON_VALUE)])
	fun createProducer(
			@ApiParam("Producer dto. Should not specify id")
			@RequestBody
			dto: ProducerDto
	): ResponseEntity<Any> {
		//Id is auto-generated and should not be specified
		if (dto.id != null) {
			return ResponseEntity.status(400).body("Id should not be specified")
		}

		producerRepo.save(Producer(name = dto.name))

		return ResponseEntity.status(201).build()
	}

	//PATCH
	@ApiOperation("Update name of producer")
	@PatchMapping(path = ["/{id}"], consumes = [MediaType.TEXT_PLAIN_VALUE])
	fun updateProducerName(
			@ApiParam("Id of producer")
			@PathVariable("id")
			pathId: Long,
			@ApiParam("The new name for the producer")
			@RequestBody
			name: String
	): ResponseEntity<Any> {
		if (!producerRepo.exists(pathId))
			return ResponseEntity.status(404).body("producer with id: $pathId not found")

		val newProducer = producerRepo.save(Producer(id = pathId, name = name))

		return ResponseEntity.ok(ProducerConverter.transform(newProducer))
	}

	//PUT
	@ApiOperation("Update an existing producer")
	@PutMapping(path = ["/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun updateProducer(
			@ApiParam("Id of producer")
			@PathVariable("id")
			pathId: Long,
			@ApiParam("The new producer which will replace the old one")
			@RequestBody
			requestDto: ProducerDto
	): ResponseEntity<Any> {
		if (requestDto.id != null) {
			return ResponseEntity.status(400).body("Id should not be specified")
		}

		if (!producerRepo.exists(pathId))
			return ResponseEntity.status(404).body("Producer with id: $pathId not found")

		val newProducer = producerRepo.save(Producer(name = requestDto.name))

		return ResponseEntity.ok(ProducerConverter.transform(newProducer))
	}

	//DELETE
	@ApiOperation("Delete producer")
	@DeleteMapping(path = ["/{id}"])
	fun deleteProducer(
			@ApiParam("Id of producer")
			@PathVariable("id")
			pathId: Long
	): ResponseEntity<Any> {
		if (!producerRepo.exists(pathId))
			return ResponseEntity.status(404).body("Producer with id: $pathId not found")

		producerRepo.delete(pathId)

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