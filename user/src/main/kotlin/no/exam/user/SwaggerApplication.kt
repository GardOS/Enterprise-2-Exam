package no.exam.user

import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
	System.setProperty("spring.profiles.active", "swagger")
	SpringApplication.run(UserApplication::class.java, *args)
}