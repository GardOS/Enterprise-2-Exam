package no.gardos.endToEnd

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.builder.RequestSpecBuilder
import io.restassured.http.ContentType
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
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

			await().atMost(300, TimeUnit.SECONDS)
					.pollInterval(Duration.FIVE_SECONDS)
					.ignoreExceptions()
					.until({
						// zuul and eureka is up when 200 is returned
						// this will in itself act as a test proving both zuul and eureka works
						given().get("http://localhost/producer-server/health").then().body("status", equalTo("UP"))
						given().get("http://localhost/consumer-server/health").then().body("status", equalTo("UP"))
						// need to make sure the data is created before running this tests
						//given().get("http://localhost/quiz-server/quizzes").then().body("size()", equalTo(3))

						true
					})
			authenticate()
		}

		private fun authenticate() {
			val token = given().contentType(ContentType.URLENC)
					.formParam("username", "username")
					.formParam("password", "password")
					.post("/signIn")
					.then()
					.statusCode(403)
					.extract().cookie("XSRF-TOKEN")

			val session = given().contentType(ContentType.URLENC)
					.formParam("username", "username")
					.formParam("password", "password")
					.header("X-XSRF-TOKEN", token)
					.cookie("XSRF-TOKEN", token)
					.post("/signIn")
					.then()
					.statusCode(204)
					.extract().cookie("SESSION")

			RestAssured.requestSpecification = RequestSpecBuilder()
					.setAccept(ContentType.JSON)
					.setContentType(ContentType.JSON)
					.addHeader("X-XSRF-TOKEN", token)
					.addCookie("XSRF-TOKEN", token)
					.addCookie("SESSION", Pair(session, token).first)
					.build()
		}
	}

	@Test
	fun completeGame() {
		assertTrue(true)
	}
}