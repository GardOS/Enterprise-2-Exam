package no.gardos.consumer

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.cloud.netflix.ribbon.RibbonClient

@SpringBootApplication
@EnableEurekaClient
@RibbonClient(name = "producer-server")
class ConsumerApplication

fun main(args: Array<String>) {
	SpringApplication.run(ConsumerApplication::class.java, *args)
}