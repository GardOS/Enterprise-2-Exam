package no.exam.seller.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.exam.schema.SaleDto
import no.exam.schema.SellerDto
import no.exam.seller.model.Seller
import no.exam.seller.model.SellerConverter
import no.exam.seller.model.SellerRepository
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

@Api(value = "/sellers", description = "API for sellers")
@RequestMapping(
		path = ["/sellers"],
		produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
@RestController
@Validated
class SellerController {

	@Autowired
	private lateinit var sellerRepo: SellerRepository

	@RabbitListener(queues = ["#{userCreatedQueue.name}"])
	fun userCreatedEvent(seller: SellerDto) {
		try {
			sellerRepo.save(
					Seller(
							username = seller.username,
							name = seller.name,
							email = seller.email,
							sales = mutableListOf()
					)
			)
		} catch (e: Exception) {
		}
	}

	@RabbitListener(queues = ["#{saleCreatedQueue.name}"])
	fun saleCreatedEvent(sale: SaleDto) {
		try {
			val user = sellerRepo.findOne(sale.seller)
			user.sales!!.add(sale.id!!)
			sellerRepo.save(user)
		} catch (ex: Exception) {
		}
	}

	@RabbitListener(queues = ["#{saleDeletedQueue.name}"])
	fun saleDeletedEvent(sale: SaleDto) {
		try {
			val user = sellerRepo.findOne(sale.seller)
			user.sales!!.remove(sale.id)
			sellerRepo.save(user)
		} catch (ex: Exception) {
		}
	}

	@ApiOperation("Get all the sellers")
	@GetMapping
	fun getAllSellers(): ResponseEntity<List<SellerDto>> {
		return ResponseEntity.ok(SellerConverter.transform(sellerRepo.findAll()))
	}

	@ApiOperation("Get seller by username")
	@GetMapping(path = ["/{username}"])
	fun getSellerByUsername(
			@ApiParam("Username of seller")
			@PathVariable("username")
			pathId: String
	): ResponseEntity<Any> {
		if (!sellerRepo.exists(pathId))
			return ResponseEntity.status(404).build()

		val user = sellerRepo.findOne(pathId)
		return ResponseEntity.ok(SellerConverter.transform(user))
	}

	//Catches validation errors and returns error status based on error
	@ExceptionHandler(value = ([JavaxConstraintViolationException::class, HibernateConstraintViolationException::class,
		DataIntegrityViolationException::class, TransactionSystemException::class]))
	fun handleValidationFailure(ex: Exception, response: HttpServletResponse): String {
		var cause: Throwable? = ex
		for (i in 0..4) { //Iterate 5 times max, since it might have infinite depth
			if (cause is JavaxConstraintViolationException || cause is HibernateConstraintViolationException) {
				response.status = HttpStatus.BAD_REQUEST.value()
				return "Invalid request"
			}
			cause = cause?.cause
		}
		response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
		return "Something went wrong processing the request"
	}
}