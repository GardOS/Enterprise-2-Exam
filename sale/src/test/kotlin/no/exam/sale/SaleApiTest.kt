package no.exam.sale

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.restassured.RestAssured
import io.restassured.RestAssured.delete
import io.restassured.RestAssured.get
import io.restassured.http.ContentType
import no.exam.sale.model.Sale
import no.exam.sale.model.SaleRepository
import no.exam.schema.SaleDto
import org.hamcrest.Matchers.equalTo
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.EnvironmentTestUtils
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.GenericContainer

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [(SaleApplication::class)])
@ContextConfiguration(initializers = [(SaleApiTest.Companion.Initializer::class)])
@ActiveProfiles("test")
class SaleApiTest {

	companion object {
		//Only reason this is included is to avoid crash when sending MQ messages FIXME
		class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

		@ClassRule
		@JvmField
		val rabbitMQ = KGenericContainer("rabbitmq:3").withExposedPorts(5672)

		class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
			override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
				EnvironmentTestUtils.addEnvironment(
						"testcontainers",
						configurableApplicationContext.environment,
						"spring.rabbitmq.host=" + rabbitMQ.containerIpAddress,
						"spring.rabbitmq.port=" + rabbitMQ.getMappedPort(5672)
				)
			}
		}

		lateinit var wireMockServer: WireMockServer

		@BeforeClass
		@JvmStatic
		fun initClass() {
			RestAssured.baseURI = "http://localhost"
			RestAssured.basePath = "/sales"
			RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
			RestAssured.authentication = RestAssured.basic("testUser", "pwd")

			//TODO: Remember port overlap
			wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(8099)
					.notifier(ConsoleNotifier(true)))
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
			seller: String = defaultSeller,
			book: Long = defaultBook,
			price: Int = defaultPrice,
			condition: String = defaultCondition
	): Sale {
		return saleRepo.save(Sale(
				seller = seller,
				book = book,
				price = price,
				condition = condition)
		)
	}

	@LocalServerPort
	protected var port = 0

	@Autowired
	protected lateinit var saleRepo: SaleRepository

	@Autowired
	private lateinit var rabbitTemplate: RabbitTemplate

	var testSale: Sale? = null
	val defaultSeller: String = "testUser"
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
	fun getSalesForSeller_receivesFilteredSales() {
		createSale(seller = defaultSeller)
		createSale(seller = "newUser")

		assertEquals(3, saleRepo.count())

		get("sellers/$defaultSeller").then()
				.body("size()", equalTo(2))
				.statusCode(200)

		get("sellers/newUser").then()
				.body("size()", equalTo(1))
				.statusCode(200)
	}

	@Test
	@Ignore
	fun createSale_SaleCreated() {
		assertEquals(1, saleRepo.count())

		val newSale = SaleDto(
				book = 1,
				price = 4321,
				condition = "newCondition"
		)

		wireMockServer.stubFor(
				WireMock.get(
						WireMock.urlMatching("/books/.*"))
						.willReturn(WireMock.aResponse()
								.withHeader("Content-Type", "application/json")
								.withBody("""{"id": "1"}""")))

		RestAssured.given().contentType(ContentType.JSON)
				.body(newSale)
				.post()
				.then()
				.statusCode(201)

		assertEquals(2, saleRepo.count())
	}

	@Test
	fun updateSale_SaleUpdated() {
		val updatedSale = SaleDto(
				price = 4321,
				condition = "newCondition"
		)
		RestAssured.given().contentType(ContentType.JSON)
				.body(updatedSale)
				.patch("/${testSale!!.id}")
				.then()
				.statusCode(204)

		assertEquals(4321, saleRepo.findOne(testSale!!.id).price)
		assertEquals("newCondition", saleRepo.findOne(testSale!!.id).condition)
	}

	@Test
	fun deleteSale_SaleRemoved() {
		assertEquals(1, saleRepo.count())

		delete("/${testSale!!.id}")
				.then()
				.statusCode(204)

		assertEquals(0, saleRepo.count())
	}
}