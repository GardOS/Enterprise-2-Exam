package no.exam.user

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient

@SpringBootApplication
@EnableEurekaClient
class UserApplication

fun main(args: Array<String>) {
	SpringApplication.run(UserApplication::class.java, *args)
}