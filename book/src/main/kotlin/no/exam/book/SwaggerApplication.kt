package no.exam.book

import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
	System.setProperty("spring.profiles.active", "swagger")
	SpringApplication.run(BookApplication::class.java, *args)
}