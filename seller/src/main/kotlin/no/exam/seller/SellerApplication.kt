package no.exam.seller

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class SellerApplication

fun main(args: Array<String>) {
	SpringApplication.run(SellerApplication::class.java, *args)
}