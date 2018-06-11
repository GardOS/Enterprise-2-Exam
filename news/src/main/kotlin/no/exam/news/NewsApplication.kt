package no.exam.news

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient

@SpringBootApplication
@EnableEurekaClient
class ProducerApplication

fun main(args: Array<String>) {
	SpringApplication.run(ProducerApplication::class.java, *args)
}