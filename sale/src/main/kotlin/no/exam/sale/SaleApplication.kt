package no.exam.sale

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class SaleApplication

fun main(args: Array<String>) {
	SpringApplication.run(SaleApplication::class.java, *args)
}