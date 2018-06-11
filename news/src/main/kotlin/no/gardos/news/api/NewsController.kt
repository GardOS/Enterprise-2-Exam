package no.gardos.news.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.gardos.news.model.News
import no.gardos.news.model.NewsConverter
import no.gardos.news.model.NewsRepository
import no.gardos.schema.NewsDto
import no.gardos.schema.SaleDto
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.TransactionSystemException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import javax.validation.ConstraintViolationException as JavaxConstraintViolationException

@Api(value = "/news", description = "API for news")
@RequestMapping(
		path = ["/news"],
		produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
@RestController
@Validated
class NewsController {
	@Autowired
	private lateinit var newsRepo: NewsRepository

	//RABBIT
	@RabbitListener(queues = ["#{queue.name}"])
	fun rabbitMq(sale: SaleDto) {
		try {
			newsRepo.save(
					News(
							sale = sale.id,
							sellerName = sale.user,
							bookTitle = sale.book.toString(),
							bookPrice = sale.price
					)
			)
		} catch (e: Exception) {
		}
	}

	//GET ALL
	@ApiOperation("Get all the news")
	@GetMapping
	fun getAllNews(): ResponseEntity<List<NewsDto>> {
		return ResponseEntity.ok(NewsConverter.transform(newsRepo.findAll()))
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