package no.gardos.sale

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class SwaggerApplication

fun main(args: Array<String>) {
	System.setProperty("spring.profiles.active", "swagger")
	SpringApplication.run(SwaggerApplication::class.java, *args)
}