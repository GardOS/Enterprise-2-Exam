package no.exam.endToEnd

import io.restassured.RestAssured
import io.restassured.RestAssured.*
import io.restassured.builder.RequestSpecBuilder
import io.restassured.http.ContentType
import no.exam.schema.BookDto
import no.exam.schema.NewsDto
import no.exam.schema.SaleDto
import no.exam.schema.SellerDto
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.h2.util.New
import org.hamcrest.CoreMatchers.equalTo
import org.junit.*
import org.junit.Assert.*
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.util.concurrent.TimeUnit

class GameFlowIT {
	val BOOK_PATH = "/book-server/books"
	val SALE_PATH = "/sale-server/sales"
	val SELLER_PATH = "/seller-server/sellers"
	val NEWS_PATH = "/news-server/news"

	companion object {
		class KDockerComposeContainer(path: File) : DockerComposeContainer<KDockerComposeContainer>(path)

		@ClassRule
		@JvmField
		val env = KDockerComposeContainer(File("../docker-compose.yml")).withLocalCompose(true)

		@BeforeClass
		@JvmStatic
		fun initialize() {
			RestAssured.baseURI = "http://localhost"
			RestAssured.port = 8080
			RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()

			await().atMost(600, TimeUnit.SECONDS)
					.pollInterval(Duration.FIVE_SECONDS)
					.ignoreExceptions()
					.until({
						//Ensure that everything is up and running before testing
						//This will in itself act as a test for Gateway and Eureka
						given().get("http://localhost/book-server/health").then().body("status", equalTo("UP"))
						given().get("http://localhost/sale-server/health").then().body("status", equalTo("UP"))
						given().get("http://localhost/seller-server/health").then().body("status", equalTo("UP"))
						given().get("http://localhost/news-server/health").then().body("status", equalTo("UP"))
						// need to make sure the data is created before running this tests
						//given().get("http://localhost/quiz-server/quizzes").then().body("size()", equalTo(3))

						true
					})
		}
	}

	@Before
	fun init() {
		get("/logout")
		RestAssured.requestSpecification = RequestSpecBuilder()
				.setAccept(ContentType.JSON)
				.setContentType(ContentType.JSON)
				.build()
	}

	fun login(username: String, password: String) {
		val token = get("/logout")
				.then()
				.extract().cookie("XSRF-TOKEN")

		val session = given().contentType(ContentType.URLENC)
				.header("X-XSRF-TOKEN", token)
				.cookie("XSRF-TOKEN", token)
				.formParam("username", username)
				.formParam("password", password)
				.post("/login")
				.then()
				.statusCode(204)
				.extract().cookie("SESSION")

		//Session and token is stored in browser
		RestAssured.requestSpecification = RequestSpecBuilder()
				.setAccept(ContentType.JSON)
				.setContentType(ContentType.JSON)
				.addHeader("X-XSRF-TOKEN", token)
				.addCookie("XSRF-TOKEN", token)
				.addCookie("SESSION", session)
				.build()
	}

	/*
	This e2e test goes through the authentication features.
	Follow an user that tests out the register, login, logout, authUser endpoints
	 */
	@Test
	fun authenticationTest() {
		val token = get("/logout")
				.then()
				.extract().cookie("XSRF-TOKEN")

		//Register user. This triggers a MQ message that a new user has been created
		val session = given().contentType(ContentType.URLENC)
				.formParam("username", "testUser")
				.formParam("password", "password")
				.formParam("name", "test name")
				.formParam("email", "test@test.com")
				.header("X-XSRF-TOKEN", token)
				.cookie("XSRF-TOKEN", token)
				.post("/register")
				.then()
				.statusCode(204)
				.extract().cookie("SESSION")

		//Get session and token
		RestAssured.requestSpecification = RequestSpecBuilder()
				.setAccept(ContentType.JSON)
				.setContentType(ContentType.JSON)
				.addHeader("X-XSRF-TOKEN", token)
				.addCookie("XSRF-TOKEN", token)
				.addCookie("SESSION", session)
				.build()

		//User looks at user page
		get("/authUser")
				.then()
				.statusCode(200)

		//User logs out
		get("/logout")
				.then()
				.statusCode(204)

		//User tries to open previous page. Permission denied
		get("/authUser")
				.then()
				.statusCode(401)

		//User logs on the account just created
		val newSession = given().contentType(ContentType.URLENC)
				.formParam("username", "testUser")
				.formParam("password", "password")
				.post("/login")
				.then()
				.statusCode(204)
				.extract().cookie("SESSION")

		//Session stored, also reset old values
		RestAssured.requestSpecification = RequestSpecBuilder()
				.setAccept(ContentType.JSON)
				.setContentType(ContentType.JSON)
				.addCookie("SESSION", newSession)
				.build()

		//User can now look at user page again
		val newToken = get("/authUser")
				.then()
				.statusCode(200)
				.extract().cookie("XSRF-TOKEN")

		//Verify that RabbitMQ message got posted. Results in seller entity being created
		await().atMost(30, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					assertEquals("testUser", given()
							.header("X-XSRF-TOKEN", newToken)
							.cookie("XSRF-TOKEN", newToken)
							.get("/seller-server/sellers/testUser")
							.then()
							.extract()
							.path<String>("username"))
					true
				})
	}

	/*
	This e2e test goes through features relevant for when a user discovers a book for sale and the proceeds to buy it.
	 */
	@Test
	fun buyerUserStory() {
		val token = get("/logout")
				.then()
				.extract().cookie("XSRF-TOKEN")

		//User checks latest news from news-feed and finds a specific news interesting
		val news = given().param("getLatest", true)
				.get(NEWS_PATH)
				.then()
				.statusCode(200)
				.extract().body()
				.`as`(Array<NewsDto>::class.java).first { n -> n.bookPrice == 700 }

		//User checks out sale details
		val sale = get("$SALE_PATH/${news.sale}")
				.then()
				.statusCode(200)
				.extract()
				.`as`(SaleDto::class.java)

		//User checks out book details
		val book = get("$BOOK_PATH/${sale.book}")
				.then()
				.statusCode(200)
				.extract()
				.`as`(BookDto::class.java)

		//User checks if there are any better offers. Finds one
		val cheaperSale = get("$SALE_PATH/books/${book.id}")
				.then()
				.statusCode(200)
				.extract().body()
				.`as`(Array<SaleDto>::class.java).first { s -> s.price == 300 }

		//User checks out the seller selling the book
		val seller = get("$SELLER_PATH/${cheaperSale.seller}")
				.then()
				.statusCode(200)
				.extract().body()
				.`as`(SellerDto::class.java)


		//User is curious if seller got other good deals and checks. Found none interesting
		get("$SALE_PATH/sellers/${seller.username}")
				.then()
				.statusCode(200)

		//Book is sold

		//Seller tries to remove sale. Forgot to log in. Token included for accurate response
		given().header("X-XSRF-TOKEN", token)
				.cookie("XSRF-TOKEN", token)
				.delete("$SALE_PATH/${cheaperSale.id}")
				.then()
				.statusCode(401)

		//Seller logs in
		login(seller.username!!, "pwd")

		//Seller removes book
		delete("$SALE_PATH/${cheaperSale.id}")
				.then()
				.statusCode(204)

		//Wait for MQ-message
		await().atMost(
				10, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					//Verify news is removed from news-feed
					assertNull(get(NEWS_PATH)
							.then()
							.statusCode(200)
							.extract().body()
							.`as`(Array<NewsDto>::class.java).find { n -> n.sale == cheaperSale.id }
					)
					true
				})

		//Wait for MQ-message then
		await().atMost(10, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					//Verify sale is removed from seller
					assertNull(get("$SELLER_PATH/${seller.username}")
							.then()
							.statusCode(200)
							.extract().`as`(SellerDto::class.java)
							.sales!!.find { equals(cheaperSale.id) }
					)
					true
				})
	}

	/*
	This e2e test goes through features relevant for when a user wants to sell a book
	*/
	@Test
	fun sellerUserStory() {
		//Get token
		val token = get("/logout")
				.then()
				.extract().cookie("XSRF-TOKEN")

		//User finds book already in system,
		val sale = SaleDto(
				book = 2,
				price = 1234,
				condition = "Perfect"
		)

		//User creates sale for book. Forgot to log in
		given().header("X-XSRF-TOKEN", token)
				.cookie("XSRF-TOKEN", token)
				.body(sale).post(SALE_PATH)
				.then()
				.statusCode(401)

		//User logs in
		login("user", "pwd")

		//Unstable in test, retry a bit
		await().atMost(
				20, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					//Creates sale again not that user is logged in
					given().body(sale).post(SALE_PATH)
							.then()
							.statusCode(201)
					true
				})

		//Refresh sale, now with ID
		val newSale = get(SALE_PATH)
				.then()
				.statusCode(200)
				.extract()
				.body().`as`(Array<SaleDto>::class.java).last()

		//Verify that we got the correct sale. Bit overkill, but gives MQ time to get/send messages
		assertEquals(sale.book, newSale.book)
		assertEquals(sale.price, newSale.price)
		assertEquals(sale.condition, newSale.condition)

		//Verify seller added sale
		val seller = get("$SELLER_PATH/${newSale.seller}")
				.then()
				.statusCode(200)
				.extract().body()
				.`as`(SellerDto::class.java)

		assert(seller.sales!!.contains(newSale.id))

		//Verify that news have been created for the sale
		var news: NewsDto? = null
		await().atMost(
				10, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					//News updated
					news = get(NEWS_PATH)
							.then()
							.statusCode(200)
							.extract().body()
							.`as`(Array<NewsDto>::class.java).find { n -> n.sale == newSale.id }
					assertNotNull(news)
					true
				})

		//Time goes on, nobody buys the book. Book deteriorates, seller updates condition and price
		given().body(SaleDto(price = 123, condition = "Bad"))
				.patch("$SALE_PATH/${newSale.id}")
				.then()
				.statusCode(204)

		//Wait for MQ-message
		var updatedNews: NewsDto? = null
		await().atMost(
				10, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					//News updated
					updatedNews = get(NEWS_PATH)
							.then()
							.statusCode(200)
							.extract().body()
							.`as`(Array<NewsDto>::class.java).find { n -> n.sale == newSale.id }
					assertNotNull(updatedNews)
					true
				})


		assertNotEquals(news?.bookPrice, updatedNews?.bookPrice)
		assertNotEquals(news?.bookCondition, updatedNews?.bookCondition)

		//Buyer sees cheap book and buys it. Seller removes sale
		delete("$SALE_PATH/${newSale.id}")
				.then()
				.statusCode(204)

		//Wait for MQ-message
		await().atMost(
				10, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					//Verify news is removed from news-feed
					assertNull(get(NEWS_PATH)
							.then()
							.statusCode(200)
							.extract().body()
							.`as`(Array<NewsDto>::class.java).find { n -> n.sale == newSale.id }
					)
					true
				})

		//Wait for MQ-message then
		await().atMost(10, TimeUnit.SECONDS)
				.pollInterval(Duration.ONE_SECOND)
				.ignoreExceptions()
				.until({
					//Verify sale is removed from seller
					assertNull(get("$SELLER_PATH/${seller.username}")
							.then()
							.statusCode(200)
							.extract().`as`(SellerDto::class.java)
							.sales!!.find { equals(newSale.id) }
					)
					true
				})
	}
}