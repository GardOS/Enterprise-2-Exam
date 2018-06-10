package no.gardos.consumer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.restassured.RestAssured
import io.restassured.RestAssured.*
import io.restassured.http.ContentType
import no.gardos.consumer.model.Consumer
import no.gardos.consumer.model.ConsumerRepository
import no.gardos.schema.ConsumerDto
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = [(ConsumerApplication::class)])
@ActiveProfiles("test")
class ConsumerApiTest {

	companion object {
		lateinit var wireMockServer: WireMockServer

		@BeforeClass
		@JvmStatic
		fun initClass() {
			RestAssured.baseURI = "http://localhost"
			RestAssured.port = 8080
			RestAssured.basePath = "/consumer" //FIXME
			RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
			RestAssured.authentication = RestAssured.basic("testUser", "pwd")

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
		consumerRepo.deleteAll() //TODO: Investigate dirty annotation for same effect
		testConsumer = consumerRepo.save(Consumer(name = "testName"))
	}

	@Autowired
	protected lateinit var consumerRepo: ConsumerRepository

	var testConsumer: Consumer? = null

	fun mockJsonString(): String {
		return """{"id": 1, "name": "mockName"}"""
	}

	@Test
	fun testWireMock() {
		val json = mockJsonString()

		wireMockServer.stubFor(
				WireMock.get(
						WireMock.urlMatching("/consumer/.*"))
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
		get("/${testConsumer?.id}")
				.then()
				.statusCode(200)
	}

	//POST
	@Test
	fun post() {
		val consumer = ConsumerDto(name = "name")

		given().contentType(ContentType.JSON)
				.body(consumer)
				.post()
				.then()
				.statusCode(201)
	}

	//PATCH
	@Test
	fun patch() {
		given().contentType(ContentType.TEXT)
				.body("newName")
				.patch("/${testConsumer?.id}")
				.then()
				.statusCode(200)
	}

	//PUT
	@Test
	fun put() {
		val consumer = ConsumerDto(name = "newName")

		given().contentType(ContentType.JSON)
				.body(consumer)
				.put("/${testConsumer?.id}")
				.then()
				.statusCode(200)
	}

	//DELETE
	@Test
	fun delete() {
		delete("/${testConsumer?.id}")
				.then()
				.statusCode(204)
	}
}