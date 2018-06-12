package no.exam.book

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.restassured.RestAssured
import io.restassured.RestAssured.*
import io.restassured.http.ContentType
import no.exam.book.model.Book
import no.exam.book.model.BookRepository
import no.exam.schema.BookDto
import org.hamcrest.Matchers.equalTo
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [(BookApplication::class)])
@ActiveProfiles("test")
class BookApiTest {

	companion object {
		lateinit var wireMockServer: WireMockServer

		@BeforeClass
		@JvmStatic
		fun initClass() {
			RestAssured.baseURI = "http://localhost"
			RestAssured.basePath = "/books"
			RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
			RestAssured.authentication = RestAssured.basic("testAdmin", "pwd")

			wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(8099).notifier(ConsoleNotifier(true)))
			wireMockServer.start()
		}

		@AfterClass
		@JvmStatic
		fun tearDown() {
			wireMockServer.stop()
		}
	}

	@Before
	fun init() {
		RestAssured.port = port

		bookRepo.deleteAll()
		testBook = bookRepo.save(
				Book(
						title = defaultTitle,
						author = defaultAuthor,
						edition = defaultEdition
				)
		)
	}

	@LocalServerPort
	protected var port = 0

	@Autowired
	protected lateinit var bookRepo: BookRepository

	var testBook: Book? = null
	val defaultTitle: String = "DefaultTitle"
	val defaultAuthor: String = "DefaultAuthor"
	val defaultEdition: String = "DefaultEdition"

	//GET ALL
	@Test
	fun getAllBooks_receivesMultiple() {
		get().then()
				.body("size()", equalTo(1))
				.statusCode(200)

		bookRepo.save(
				Book(
						title = defaultTitle,
						author = defaultAuthor,
						edition = defaultEdition
				)
		)

		get().then()
				.body("size()", equalTo(2))
				.statusCode(200)
	}

	//GET ONE
	@Test
	fun getBook_receivesBook() {
		val book = get("/${testBook?.id}")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.`as`(BookDto::class.java)

		assertEquals(testBook!!.title, book.title)
	}

	//POST
	@Test
	fun createBook_bookCreated() {
		assertEquals(1, bookRepo.count())

		val book = BookDto(
				title = "NewTitle",
				author = "NewAuthor",
				edition = "NewEdition"
		)

		given().contentType(ContentType.JSON)
				.body(book)
				.post()
				.then()
				.statusCode(201)

		assertEquals(2, bookRepo.count())
	}

	//PUT
	@Test
	fun replaceBook_bookUpdated() {
		assertEquals(defaultTitle, bookRepo.findOne(testBook!!.id).title)

		val newBook = BookDto(
				id = testBook?.id,
				title = "NewTitle",
				author = "NewAuthor",
				edition = "NewEdition"
		)

		given().contentType(ContentType.JSON)
				.body(newBook)
				.put("/${testBook?.id}")
				.then()
				.statusCode(204)

		assertEquals("NewTitle", bookRepo.findOne(testBook!!.id).title)
	}

	@Test
	fun replaceBook_bookCreated() {
		val newBook = BookDto(
				id = 1234,
				title = "NewTitle",
				author = "NewAuthor"
		)

		given().contentType(ContentType.JSON)
				.body(newBook)
				.put("/1234")
				.then()
				.statusCode(201)
	}

	//PATCH
	@Test
	fun updateBook_fieldsChanged() {
		assertEquals(defaultTitle, bookRepo.findOne(testBook!!.id).title)

		val newBookString = """{
			"title": "NewTitle",
			"author": "NewAuthor",
			"edition": "NewEdition"
			}"""

		given().contentType(ContentType.JSON)
				.body(newBookString)
				.patch("/${testBook!!.id}")
				.then()
				.statusCode(204)

		assertEquals("NewTitle", bookRepo.findOne(testBook!!.id).title)
	}

	@Test
	fun updateBook_partialChangeSuccessful() {
		assertEquals(defaultTitle, bookRepo.findOne(testBook!!.id).title)
		assertEquals(defaultAuthor, bookRepo.findOne(testBook!!.id).author)

		val newBookString = """{"title": "NewTitle"}"""

		given().contentType(ContentType.JSON)
				.body(newBookString)
				.patch("/${testBook?.id}")
				.then()
				.statusCode(204)

		assertEquals("NewTitle", bookRepo.findOne(testBook!!.id).title)
		assertEquals(defaultAuthor, bookRepo.findOne(testBook!!.id).author)
	}

	@Test
	fun updateBook_UpdatesWhenNullValueSet() {
		assertEquals(defaultEdition, bookRepo.findOne(testBook!!.id).edition)

		val newBookString = """{"edition": null}"""

		given().contentType(ContentType.JSON)
				.body(newBookString)
				.patch("/${testBook?.id}")
				.then()
				.statusCode(204)

		assertEquals(null, bookRepo.findOne(testBook!!.id).edition)
	}

	@Test
	fun updateBook_IgnoresMissingFields() {
		assertEquals(defaultEdition, bookRepo.findOne(testBook!!.id).edition)

		val newBookString = """{"title": "NewTitle"}"""

		given().contentType(ContentType.JSON)
				.body(newBookString)
				.patch("/${testBook?.id}")
				.then()
				.statusCode(204)

		assertEquals(defaultEdition, bookRepo.findOne(testBook!!.id).edition)
	}

	//DELETE
	@Test
	fun deleteBook_bookDeleted() {
		assertEquals(1, bookRepo.count())

		delete("/${testBook?.id}")
				.then()
				.statusCode(204)

		assertEquals(0, bookRepo.count())
	}
}