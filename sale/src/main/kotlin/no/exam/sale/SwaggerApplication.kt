package no.exam.sale

import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
	System.setProperty("spring.profiles.active", "swagger")
	SpringApplication.run(SaleApplication::class.java, *args)
}