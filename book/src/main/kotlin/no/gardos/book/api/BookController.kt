package no.gardos.book.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.gardos.book.model.Book
import no.gardos.book.model.BookConverter
import no.gardos.book.model.BookRepository
import no.gardos.schema.BookDto
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

@Api(value = "/books", description = "API for books")
@RequestMapping(
		path = ["/books"],
		produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
@RestController
@Validated
class BookController {
	@Autowired
	private lateinit var bookRepo: BookRepository

	//GET ALL
	@ApiOperation("Get all books")
	@GetMapping
	fun getAllBooks(): ResponseEntity<List<BookDto>> {
		return ResponseEntity.ok(BookConverter.transform(bookRepo.findAll()))
	}

	//GET ONE
	@ApiOperation("Get book by id")
	@GetMapping(path = ["/{id}"])
	fun getBook(
			@ApiParam("Id of the book")
			@PathVariable("id")
			pathId: Long
	): ResponseEntity<Any> {
		val book = bookRepo.findOne(pathId)
				?: return ResponseEntity.status(404).body("book with id: $pathId not found")

		return ResponseEntity.ok(BookConverter.transform(book))
	}

	//POST
	@ApiOperation("Create new book")
	@PostMapping(consumes = [(MediaType.APPLICATION_JSON_VALUE)])
	fun createBook(
			@ApiParam("Book dto. Should not specify id")
			@RequestBody
			dto: BookDto
	): ResponseEntity<Any> {
		//Id is auto-generated and should not be specified
		if (dto.id != null) {
			return ResponseEntity.status(400).body("Id should not be specified")
		}

		bookRepo.save(
				Book(
						title = dto.title,
						author = dto.author,
						condition = dto.condition
				)
		)

		return ResponseEntity.status(201).build()
	}

	//PUT
	@ApiOperation("Replace an existing book. Id will not be changed")
	@PutMapping(path = ["/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun replaceBook(
			@ApiParam("Id of the book")
			@PathVariable("id")
			pathId: Long,
			@ApiParam("The new book which will replace the old one")
			@RequestBody
			dto: BookDto
	): ResponseEntity<Any> {
		if (dto.id != pathId) {
			return ResponseEntity.status(409).body("Inconsistent id. Mismatch between path and body")
		}

		if (!bookRepo.exists(pathId))
			return ResponseEntity.status(404).body("Book with id: $pathId not found")

		bookRepo.save(
				Book(
						id = dto.id,
						title = dto.title,
						author = dto.author,
						condition = dto.condition
				)
		)

		return ResponseEntity.status(204).build()
	}

	//PATCH
	@ApiOperation("Update an existing book")
	@PatchMapping(path = ["/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun updateBook(
			@ApiParam("Id of the book")
			@PathVariable("id")
			pathId: Long,
			@ApiParam("Fields to change on the book. Id should not be specified")
			@RequestBody
			dto: BookDto //Book cant have null fields. Therefore the null/missing value problem for merge patch is not applicable
	): ResponseEntity<Any> {
		if (!bookRepo.exists(pathId))
			return ResponseEntity.status(404).body("book with id: $pathId not found")

		val updatedBook = bookRepo.findOne(pathId)

		if (dto.title != null)
			updatedBook.title = dto.title

		if (dto.author != null)
			updatedBook.author = dto.author

		if (dto.condition != null)
			updatedBook.condition = dto.condition

		bookRepo.save(updatedBook)

		return ResponseEntity.status(204).build()
	}

	//DELETE
	@ApiOperation("Delete existing book")
	@DeleteMapping(path = ["/{id}"])
	fun deleteBook(
			@ApiParam("Id of the book")
			@PathVariable("id")
			pathId: Long
	): ResponseEntity<Any> {
		if (!bookRepo.exists(pathId))
			return ResponseEntity.status(404).body("Book with id: $pathId not found")

		bookRepo.delete(pathId)

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
				return "Invalid request. Error:\n${ex.message ?: "Error not found"}" //TODO: Remove ex.message
			}
			cause = cause?.cause
		}
		response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
		return "Something went wrong processing the request.  Error:\n${ex.message
				?: "Error not found"}" //TODO: Remove ex.message
	}
}