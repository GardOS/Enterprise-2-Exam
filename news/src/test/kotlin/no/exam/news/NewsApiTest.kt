package no.exam.news

import io.restassured.RestAssured
import io.restassured.RestAssured.get
import io.restassured.RestAssured.given
import no.exam.news.model.News
import no.exam.news.model.NewsRepository
import no.exam.schema.SaleDto
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.*
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [(NewsApplication::class)])
@ContextConfiguration(initializers = [(NewsApiTest.Companion.Initializer::class)])
@ActiveProfiles("test")
class NewsApiTest {

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
			RestAssured.basePath = "/news"
			RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
		}
	}

	@Before
	fun init() {
		RestAssured.port = port
		newsRepo.deleteAll()
	}

	@LocalServerPort
	protected var port = 0

	@Autowired
	protected lateinit var newsRepo: NewsRepository

	@Autowired
	private lateinit var rabbitTemplate: RabbitTemplate

	@Autowired
	private lateinit var saleCreatedFanout: FanoutExchange

	@Autowired
	private lateinit var saleUpdatedFanout: FanoutExchange

	@Autowired
	private lateinit var saleDeletedFanout: FanoutExchange

	val defaultSellerName: String = "SellerName"
	val defaultBookTitle: String = "BookTitle"
	val defaultPrice: Int = 1000
	val defaultBookCondition: String = "BookCondition"

	fun createNews(amount: Long) {
		for (i in 1..amount) {
			newsRepo.save(
					News(
							sale = i,
							sellerName = defaultSellerName,
							bookTitle = defaultBookTitle,
							bookPrice = defaultPrice,
							bookCondition = defaultBookCondition
					)
			)
		}
	}

	@Test
	fun getAllNews_receivesMultiple() {
		createNews(2)
		get().then()
				.body("size()", equalTo(2))
				.statusCode(200)
	}

	@Test
	fun getAllNews_descendingWhenLatestFlagSet() {
		createNews(2)
		val newsIds = get()
				.then()
				.extract()
				.path<List<Long>>("sale")

		assertTrue(newsIds.first() < newsIds.last())

		val latestNewsIds = given()
				.param("getLatest", true)
				.get()
				.then()
				.extract()
				.path<List<Long>>("sale")

		assertTrue(latestNewsIds.first() > latestNewsIds.last())
	}

	@Test
	fun getAllNews_fewerNewsWhenLatestFlagSet() {
		createNews(11)
		get().then()
				.body("size()", equalTo(11))
				.statusCode(200)

		given().param("getLatest", true)
				.get()
				.then()
				.body("size()", equalTo(10))
				.statusCode(200)
	}

	@Test
	fun saleCreatedEvent_NewsCreated() {
		assertEquals(0, newsRepo.count())

		val sale = SaleDto(
				id = 1234,
				user = "testUser",
				book = 1234,
				price = defaultPrice,
				condition = defaultBookCondition
		)

		rabbitTemplate.convertAndSend(saleCreatedFanout.name, "", sale)

		await().atMost(5, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					assertEquals(1, newsRepo.count())
					true
				})

	}

	@Test
	fun saleUpdatedEvent_NewsUpdated() {
		createNews(1)

		val defaultNews = newsRepo.findOne(1)

		assertEquals(defaultPrice, defaultNews.bookPrice)
		assertEquals(defaultBookCondition, defaultNews.bookCondition)

		val sale = SaleDto(id = 1, condition = "NewCondition", price = 2000)

		rabbitTemplate.convertAndSend(saleUpdatedFanout.name, "", sale)

		await().atMost(5, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					assertNotEquals(defaultPrice, newsRepo.findOne(1).bookPrice)
					assertNotEquals(defaultBookCondition, newsRepo.findOne(1).bookCondition)
					true
				})
	}

	@Test
	fun saleDeletedEvent_NewsUpdated() {
		createNews(1)

		assertEquals(1, newsRepo.count())

		val sale = SaleDto(id = 1)

		rabbitTemplate.convertAndSend(saleDeletedFanout.name, "", sale)

		await().atMost(5, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					assertEquals(0, newsRepo.count())
					true
				})
	}
}