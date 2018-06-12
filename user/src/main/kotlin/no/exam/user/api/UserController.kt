package no.exam.user.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.exam.schema.SaleDto
import no.exam.schema.UserDto
import no.exam.user.model.User
import no.exam.user.model.UserConverter
import no.exam.user.model.UserRepository
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

@Api(value = "/users", description = "API for users")
@RequestMapping(
		path = ["/users"],
		produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
@RestController
@Validated
class UserController {
	@Autowired
	private lateinit var userRepo: UserRepository

	//RABBIT
	@RabbitListener(queues = ["#{userCreatedQueue.name}"])
	fun userCreatedEvent(user: UserDto) {
		try {
			userRepo.save(
					User(
							username = user.username,
							name = user.name,
							email = user.email,
							sales = mutableListOf()
					)
			)
		} catch (e: Exception) {
		}
	}

	@RabbitListener(queues = ["#{saleCreatedQueue.name}"])
	fun saleCreatedEvent(sale: SaleDto) {
		try {
			val user = userRepo.findOne(sale.user)
			user.sales!!.add(sale.id!!)
			userRepo.save(user)
		} catch (ex: Exception) {
		}
	}

	@RabbitListener(queues = ["#{saleDeletedQueue.name}"])
	fun saleDeletedEvent(sale: SaleDto) {
		try {
			val user = userRepo.findOne(sale.user)
			user.sales!!.remove(sale.id)
			userRepo.save(user)
		} catch (ex: Exception) {
			println(ex.message)
		}
	}

	@ApiOperation("Get all the users")
	@GetMapping
	fun getAllUsers(): ResponseEntity<List<UserDto>> {
		return ResponseEntity.ok(UserConverter.transform(userRepo.findAll()))
	}

	@ApiOperation("Get user by username")
	@GetMapping(path = ["/{username}"])
	fun getUserByUsername(
			@ApiParam("Username of user")
			@PathVariable("username")
			pathId: String
	): ResponseEntity<Any> {
		if (!userRepo.exists(pathId))
			return ResponseEntity.status(404).build()

		val user = userRepo.findOne(pathId)
		return ResponseEntity.ok(UserConverter.transform(user))
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