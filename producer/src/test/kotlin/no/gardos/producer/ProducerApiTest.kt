package no.gardos.producer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.restassured.RestAssured
import io.restassured.RestAssured.*
import io.restassured.http.ContentType
import no.gardos.producer.model.Producer
import no.gardos.producer.model.ProducerRepository
import no.gardos.schema.ProducerDto
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = [(ProducerApplication::class)])
@ActiveProfiles("test")
class ProducerApiTest {

	companion object {
		lateinit var wireMockServer: WireMockServer

		@BeforeClass
		@JvmStatic
		fun initClass() {
			RestAssured.baseURI = "http://localhost"
			RestAssured.port = 8080
			RestAssured.basePath = "/producer" //FIXME
			RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
			RestAssured.authentication = RestAssured.basic("testUser", "pwd")

			wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(8100).notifier(ConsoleNotifier
			(true)))
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
		producerRepo.deleteAll() //TODO: Investigate dirty annotation for same effect
		testProducer = producerRepo.save(Producer(name = "testName"))
	}

	@Autowired
	protected lateinit var producerRepo: ProducerRepository

	var testProducer: Producer? = null

	fun mockJsonString(): String {
		return """{"id": 1, "name": "mockName"}"""
	}

	@Test
	fun testWireMock() {
		val json = mockJsonString()

		wireMockServer.stubFor(
				WireMock.get(
						WireMock.urlMatching("/producer/.*"))
						.willReturn(WireMock.aResponse()
								.withHeader("Content-Type", "application/json")
								.withBody(json)))

		RestAssured.given().contentType(ContentType.JSON)
				.get()
				.then()
				.statusCode(200)
	}

	//GET ALL
	@Test
	fun getAll() {
		get().then().statusCode(200)
	}

	//GET ONE
	@Test
	fun getOne() {
		get("/${testProducer?.id}")
				.then()
				.statusCode(200)
	}

	//POST
	@Test
	fun post() {
		val producer = ProducerDto(name = "name")

		given().contentType(ContentType.JSON)
				.body(producer)
				.post()
				.then()
				.statusCode(201)
	}

	//PATCH
	@Test
	fun patch() {
		given().contentType(ContentType.TEXT)
				.body("newName")
				.patch("/${testProducer?.id}")
				.then()
				.statusCode(200)
	}

	//PUT
	@Test
	fun put() {
		val producer = ProducerDto(name = "newName")

		given().contentType(ContentType.JSON)
				.body(producer)
				.put("/${testProducer?.id}")
				.then()
				.statusCode(200)
	}

	//DELETE
	@Test
	fun delete() {
		delete("/${testProducer?.id}")
				.then()
				.statusCode(204)
	}
}