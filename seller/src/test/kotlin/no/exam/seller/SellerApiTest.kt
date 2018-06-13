package no.exam.seller

import io.restassured.RestAssured
import io.restassured.RestAssured.get
import no.exam.schema.SaleDto
import no.exam.schema.SellerDto
import no.exam.seller.model.Seller
import no.exam.seller.model.SellerConverter
import no.exam.seller.model.SellerRepository
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.amqp.core.FanoutExchange
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
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [(SellerApplication::class)])
@ContextConfiguration(initializers = [(SellerApiTest.Companion.Initializer::class)])
@ActiveProfiles("test")
class SellerApiTest {

	companion object {
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

		@BeforeClass
		@JvmStatic
		fun initClass() {
			RestAssured.baseURI = "http://localhost"
			RestAssured.basePath = "/sellers"
			RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
		}
	}

	@Before
	fun init() {
		RestAssured.port = port

		sellerRepo.deleteAll()
		testSeller = sellerRepo.save(
				Seller(
						username = defaultUsername,
						name = defaultName,
						email = defaultEmail,
						sales = mutableListOf(1, 2)
				)
		)
	}

	@LocalServerPort
	protected var port = 0

	@Autowired
	protected lateinit var sellerRepo: SellerRepository

	@Autowired
	private lateinit var rabbitTemplate: RabbitTemplate

	@Autowired
	private lateinit var userCreatedFanout: FanoutExchange

	@Autowired
	private lateinit var saleCreatedFanout: FanoutExchange

	@Autowired
	private lateinit var saleDeletedFanout: FanoutExchange

	var testSeller: Seller? = null
	val defaultUsername: String = "defaultUsername"
	val defaultName: String = "defaultName"
	val defaultEmail: String = "defaultEmail@email.com"

	@Test
	fun getAllSellers_receivesMultiple() {
		get().then()
				.body("size()", equalTo(1))
				.statusCode(200)

		testSeller!!.username = "newUsername"

		sellerRepo.save(testSeller)

		get().then()
				.body("size()", equalTo(2))
				.statusCode(200)
	}

	@Test
	fun getSellerByUsername_receivesUser() {
		val seller = get("/${testSeller!!.username}")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.`as`(SellerDto::class.java)

		assertEquals(testSeller!!.name, seller.name)
	}

	@Test
	fun sellerCreatedEvent_UserCreated() {
		sellerRepo.delete(testSeller)

		testSeller!!.sales = null //Fails to serialize with mutableList

		val sellerDto = SellerConverter.transform(testSeller!!)

		rabbitTemplate.convertAndSend(userCreatedFanout.name, "", sellerDto)

		await().atMost(5, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					assertEquals(1, sellerRepo.count())
					true
				})

	}

	@Test
	fun saleCreatedEvent_SaleAdded() {
		assertEquals(2, testSeller!!.sales!!.size)

		val sale = SaleDto(
				id = 3,
				seller = defaultUsername,
				book = 1234,
				price = 1234,
				condition = "condition"
		)

		rabbitTemplate.convertAndSend(saleCreatedFanout.name, "", sale)

		await().atMost(5, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					assertEquals(3, sellerRepo.findOne(testSeller!!.username).sales!!.size)
					true
				})

	}

	@Test
	fun saleDeletedEvent_SaleRemoved() {
		assertEquals(2, testSeller!!.sales!!.size)

		val sale = SaleDto(
				id = 2,
				seller = defaultUsername,
				book = 1234,
				price = 1234,
				condition = "condition"
		)

		rabbitTemplate.convertAndSend(saleDeletedFanout.name, "", sale)

		await().atMost(5, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					assertEquals(1, sellerRepo.findOne(testSeller!!.username).sales!!.size)
					true
				})

	}
}