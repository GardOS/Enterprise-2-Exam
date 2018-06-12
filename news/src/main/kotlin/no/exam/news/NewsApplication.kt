package no.exam.news

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class NewsApplication

fun main(args: Array<String>) {
	SpringApplication.run(NewsApplication::class.java, *args)
}