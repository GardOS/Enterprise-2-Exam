package no.exam.user

import io.restassured.RestAssured
import io.restassured.RestAssured.get
import no.exam.schema.SaleDto
import no.exam.schema.UserDto
import no.exam.user.model.User
import no.exam.user.model.UserConverter
import no.exam.user.model.UserRepository
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.GenericContainer
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [(UserApplication::class)])
@ActiveProfiles("test")
class UserApiTest {

	companion object {
		class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

		@ClassRule
		@JvmField
		val rabbitMQ = KGenericContainer("rabbitmq:3").withExposedPorts(5672)!!

		@BeforeClass
		@JvmStatic
		fun initClass() {
			RestAssured.baseURI = "http://localhost"
			RestAssured.basePath = "/users"
			RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
		}
	}

	@Before
	fun init() {
		RestAssured.port = port

		userRepo.deleteAll()
		testUser = userRepo.save(
				User(
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
	protected lateinit var userRepo: UserRepository

	@Autowired
	private lateinit var rabbitTemplate: RabbitTemplate

	@Autowired
	private lateinit var userCreatedFanout: FanoutExchange

	@Autowired
	private lateinit var saleCreatedFanout: FanoutExchange

	@Autowired
	private lateinit var saleDeletedFanout: FanoutExchange

	var testUser: User? = null
	val defaultUsername: String = "defaultUsername"
	val defaultName: String = "defaultName"
	val defaultEmail: String = "defaultEmail@email.com"

	@Test
	fun getAllUsers_receivesMultiple() {
		get().then()
				.body("size()", equalTo(1))
				.statusCode(200)

		testUser!!.username = "newUsername"

		userRepo.save(testUser)

		get().then()
				.body("size()", equalTo(2))
				.statusCode(200)
	}

	@Test
	fun getUserByUsername_receivesUser() {
		val user = get("/${testUser!!.username}")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.`as`(UserDto::class.java)

		assertEquals(testUser!!.name, user.name)
	}

	@Test
	fun userCreatedEvent_UserCreated() {
		userRepo.delete(testUser)

		testUser!!.sales = null //Fails to serialize with mutableList

		val userDto = UserConverter.transform(testUser!!)

		rabbitTemplate.convertAndSend(userCreatedFanout.name, "", userDto)

		await().atMost(5, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					assertEquals(1, userRepo.count())
					true
				})

	}

	@Test
	fun saleCreatedEvent_SaleAdded() {
		assertEquals(2, testUser!!.sales!!.size)

		val sale = SaleDto(
				id = 3,
				user = defaultUsername,
				book = 1234,
				price = 1234,
				condition = "condition"
		)

		rabbitTemplate.convertAndSend(saleCreatedFanout.name, "", sale)

		await().atMost(5, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					assertEquals(3, userRepo.findOne(testUser!!.username).sales!!.size)
					true
				})

	}

	@Test
	fun saleDeletedEvent_SaleRemoved() {
		assertEquals(2, testUser!!.sales!!.size)

		val sale = SaleDto(
				id = 2,
				user = defaultUsername,
				book = 1234,
				price = 1234,
				condition = "condition"
		)

		rabbitTemplate.convertAndSend(saleDeletedFanout.name, "", sale)

		await().atMost(5, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					assertEquals(1, userRepo.findOne(testUser!!.username).sales!!.size)
					true
				})

	}
}