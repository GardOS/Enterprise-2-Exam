package no.exam.news

import io.restassured.RestAssured
import io.restassured.RestAssured.get
import io.restassured.RestAssured.given
import no.exam.news.model.News
import no.exam.news.model.NewsRepository
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = [(NewsApplication::class)])
@ActiveProfiles("test")
class NewsApiTest {

	companion object {
		@BeforeClass
		@JvmStatic
		fun initClass() {
			RestAssured.baseURI = "http://localhost"
			RestAssured.port = 8080
			RestAssured.basePath = "/news"
			RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
		}
	}

	@Before
	fun init() {
		newsRepo.deleteAll()
	}

	@Autowired
	protected lateinit var newsRepo: NewsRepository

	fun createNews(amount: Int) {
		for (i in 1..amount) {
			newsRepo.save(
					News(
							sale = 1234,
							sellerName = "SellerName",
							bookTitle = "BookTitle",
							bookPrice = 1234
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
				.path<List<Long>>("id")

		assertTrue(newsIds.first() < newsIds.last())

		val latestNewsIds = given()
				.param("getLatest", true)
				.get()
				.then()
				.extract()
				.path<List<Long>>("id")

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
}