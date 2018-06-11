package no.exam.news

import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
	System.setProperty("spring.profiles.active", "swagger")
	SpringApplication.run(NewsApplication::class.java, *args)
}