package no.exam.book

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient

@SpringBootApplication
@EnableEurekaClient
class BookApplication

fun main(args: Array<String>) {
	SpringApplication.run(BookApplication::class.java, *args)
}