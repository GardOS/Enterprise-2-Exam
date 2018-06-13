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
import org.hamcrest.CoreMatchers.equalTo
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.util.concurrent.TimeUnit

class GameFlowIT {
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

	var initToken : String? = null

	@Before
	fun init() {
		initToken = get("/logout")
				.then()
				.extract().cookie("XSRF-TOKEN")
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
	User starts at "login" page. Two buttons "Register" up or "Login".
	Since this is the first time the user is on the site the "Register" button is clicked
	 */
	@Test
	fun authenticationTest() {
		//User types in user data and clicks OK. This triggers a MQ message that a new user has been created
		val session = given().contentType(ContentType.URLENC)
				.formParam("username", "testUser")
				.formParam("password", "password")
				.formParam("name", "test name")
				.formParam("email", "test@test.com")
				.header("X-XSRF-TOKEN", initToken)
				.cookie("XSRF-TOKEN", initToken)
				.post("/register")
				.then()
				.statusCode(204)
				.extract().cookie("SESSION")

		//Session and token is stored in browser
		RestAssured.requestSpecification = RequestSpecBuilder()
				.setAccept(ContentType.JSON)
				.setContentType(ContentType.JSON)
				.addHeader("X-XSRF-TOKEN", initToken)
				.addCookie("XSRF-TOKEN", initToken)
				.addCookie("SESSION", session)
				.build()

		//User looks at his user page
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

		//User logs on the account he just created
		val newSession = given().contentType(ContentType.URLENC)
				.formParam("username", "testUser")
				.formParam("password", "password")
				.post("/login")
				.then()
				.statusCode(204)
				.extract().cookie("SESSION")

		//Session stored in browser also reset old values
		RestAssured.requestSpecification = RequestSpecBuilder()
				.setAccept(ContentType.JSON)
				.setContentType(ContentType.JSON)
				.addCookie("SESSION", newSession)
				.build()

		//User can now look at his user page again
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

	val BOOK_PATH = "/book-server/books"
	val SALE_PATH = "/sale-server/sales"
	val SELLER_PATH = "/seller-server/sellers"
	val NEWS_PATH = "/news-server/news"

	@Test
	fun bookCrud() {
		//Get all books
		get(BOOK_PATH)
				.then()
				.body("size()", equalTo(3))
				.statusCode(200)

		//Get book by id
		get("$BOOK_PATH/1")
				.then()
				.statusCode(200)

		val newBook = BookDto(title = "Title", author = "Author", edition = "5th")

		//Create book, but needs to be logged in
		given().body(newBook)
				.post(BOOK_PATH)
				.then()
				.statusCode(401)

		login("user", "pwd")

		//Authorized
		given().body(newBook)
				.post(BOOK_PATH)
				.then()
				.statusCode(201)

		//
		get(BOOK_PATH)
				.then()
				.body("size()", equalTo(4))
				.statusCode(200)
	}

	@Test
	fun buyerUserStory() {
//		User checks latest news from news-feed and finds a specific news interesting
		val news = given().param("getLatest", true)
				.get(NEWS_PATH)
				.then()
				.statusCode(200)
				.extract().body()
				.`as`(Array<NewsDto>::class.java).first { n -> n.sale == 3L }

//		Get sale by id
		val sale = get("$SALE_PATH/${news.sale}")
				.then()
				.statusCode(200)
				.extract()
				.`as`(SaleDto::class.java)

//		Get book related to sale
		val book = get("$BOOK_PATH/${sale.book}")
				.then()
				.statusCode(200)
				.extract()
				.`as`(BookDto::class.java)

//		Check all sales for book. Finds a cheaper one
		val cheaperSale = get("$SALE_PATH/books/${book.id}")
				.then()
				.statusCode(200)
				.extract().body()
				.`as`(Array<SaleDto>::class.java).first { s -> s.price == 300 }

//		Find better deal on book, check out seller
		val seller = get("$SELLER_PATH/${cheaperSale.seller}")
				.then()
				.statusCode(200)
				.extract().body()
				.`as`(SellerDto::class.java)


		//Curious if seller got other good deals. Does not
		get("$SALE_PATH/sellers/${seller.username}")
				.then()
				.statusCode(200)

		//Seller tries to remove sale. Forgot to log in. Token included for accurate response
		given().header("X-XSRF-TOKEN", initToken)
				.cookie("XSRF-TOKEN", initToken)
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
}