package no.exam.user.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.exam.user.model.User
import no.exam.user.model.UserRepository
import org.springframework.amqp.core.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Component
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableEurekaClient
@EnableSwagger2
class UserApplicationConfig {

	@Bean
	fun swaggerApi(): Docket {
		return Docket(DocumentationType.SWAGGER_2)
				.apiInfo(apiInfo())
				.select()
				.paths(PathSelectors.any())
				.apis(RequestHandlerSelectors.basePackage("no.exam.user"))
				.build()
	}

	private fun apiInfo(): ApiInfo {
		return ApiInfoBuilder()
				.title("REST API for interacting with the user")
				.version("1.0")
				.build()
	}

	@Bean(name = ["OBJECT_MAPPER_BEAN"])
	fun jsonObjectMapper(): ObjectMapper {
		return Jackson2ObjectMapperBuilder.json()
				.serializationInclusion(JsonInclude.Include.NON_NULL)
				.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.modules(JavaTimeModule())
				.build()
	}

	//User created MQ message
	@Bean
	fun userCreatedFanout(): FanoutExchange {
		return FanoutExchange("user-created")
	}

	@Bean
	fun userCreatedQueue(): Queue {
		return AnonymousQueue()
	}

	@Bean
	fun userCreatedBinding(userCreatedFanout: FanoutExchange, userCreatedQueue: Queue): Binding {
		return BindingBuilder.bind(userCreatedQueue).to(userCreatedFanout)
	}

	//Sale created MQ message
	@Bean
	fun saleCreatedFanout(): FanoutExchange {
		return FanoutExchange("sale-created")
	}

	@Bean
	fun saleCreatedQueue(): Queue {
		return AnonymousQueue()
	}

	@Bean
	fun saleCreatedBinding(saleCreatedFanout: FanoutExchange, saleCreatedQueue: Queue): Binding {
		return BindingBuilder.bind(saleCreatedQueue).to(saleCreatedFanout)
	}

	//Sale deleted MQ message
	@Bean
	fun saleDeletedFanout(): FanoutExchange {
		return FanoutExchange("sale-deleted")
	}

	@Bean
	fun saleDeletedQueue(): Queue {
		return AnonymousQueue()
	}

	@Bean
	fun saleDeletedBinding(saleDeletedFanout: FanoutExchange, saleDeletedQueue: Queue): Binding {
		return BindingBuilder.bind(saleDeletedQueue).to(saleDeletedFanout)
	}
}

@Component
internal class DataPreLoader : CommandLineRunner {
	@Autowired
	var userRepo: UserRepository? = null

	override fun run(vararg args: String) {
		userRepo!!.save(User(
				username = "admin",
				name = "admin name",
				email = "admin@admin.com",
				sales = mutableListOf()
		))

		userRepo!!.save(User(
				username = "user",
				name = "user name",
				email = "user@user.com",
				sales = mutableListOf()
		))
	}
}