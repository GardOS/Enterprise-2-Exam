package no.exam.sale

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.restassured.RestAssured
import io.restassured.RestAssured.get
import no.exam.sale.model.Sale
import no.exam.sale.model.SaleRepository
import no.exam.schema.SaleDto
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [(SaleApplication::class)])
@ActiveProfiles("test")
class SaleApiTest {

	companion object {
		lateinit var wireMockServer: WireMockServer

		@BeforeClass
		@JvmStatic
		fun initClass() {
			RestAssured.baseURI = "http://localhost"
			RestAssured.basePath = "/sales"
			RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()

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

		saleRepo.deleteAll()
		testSale = createSale()
	}

	private fun createSale(
			user: String = defaultUser,
			book: Long = defaultBook,
			price: Int = defaultPrice,
			condition: String = defaultCondition
	): Sale {
		return saleRepo.save(Sale(
				user = user,
				book = book,
				price = price,
				condition = condition)
		)
	}

	@LocalServerPort
	protected var port = 0

	@Autowired
	protected lateinit var saleRepo: SaleRepository

	var testSale: Sale? = null
	val defaultUser: String = "defaultUser"
	val defaultBook: Long = 1234
	val defaultPrice: Int = 1234
	val defaultCondition: String = "defaultCondition"

	@Test
	fun getAllSales_receivesMultiple() {
		get().then()
				.body("size()", equalTo(1))
				.statusCode(200)

		createSale()

		get().then()
				.body("size()", equalTo(2))
				.statusCode(200)
	}

	@Test
	fun getSale_receivesSale() {
		val sale = get("/${testSale!!.id}")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.`as`(SaleDto::class.java)

		assertEquals(testSale!!.id, sale.id)
	}

	@Test
	fun getSalesForBook_receivesFilteredSales() {
		createSale(book = defaultBook)
		createSale(book = 4321)

		assertEquals(3, saleRepo.count())

		get("books/$defaultBook").then()
				.body("size()", equalTo(2))
				.statusCode(200)

		get("books/4321").then()
				.body("size()", equalTo(1))
				.statusCode(200)
	}

	@Test
	fun getSalesForUser_receivesFilteredSales() {
		createSale(user = defaultUser)
		createSale(user = "newUser")

		assertEquals(3, saleRepo.count())

		get("users/$defaultUser").then()
				.body("size()", equalTo(2))
				.statusCode(200)

		get("users/newUser").then()
				.body("size()", equalTo(1))
				.statusCode(200)
	}
}