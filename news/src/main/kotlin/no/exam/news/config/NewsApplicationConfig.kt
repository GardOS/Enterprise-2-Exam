package no.exam.news.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.exam.news.model.News
import no.exam.news.model.NewsRepository
import org.springframework.amqp.core.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.cloud.netflix.ribbon.RibbonClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
@EnableEurekaClient
@RibbonClient(name = "book-server")
class NewsApplicationConfig {

	@Bean
	fun swaggerApi(): Docket {
		return Docket(DocumentationType.SWAGGER_2)
				.apiInfo(apiInfo())
				.select()
				.paths(PathSelectors.any())
				.apis(RequestHandlerSelectors.basePackage("no.exam.news"))
				.build()
	}

	private fun apiInfo(): ApiInfo {
		return ApiInfoBuilder()
				.title("REST API for interacting with the news")
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

	@Bean
	@LoadBalanced
	fun restTemplate(): RestTemplate {
		return RestTemplate()
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

	//Sale updated MQ message
	@Bean
	fun saleUpdatedFanout(): FanoutExchange {
		return FanoutExchange("sale-updated")
	}

	@Bean
	fun saleUpdatedQueue(): Queue {
		return AnonymousQueue()
	}

	@Bean
	fun saleUpdatedBinding(saleUpdatedFanout: FanoutExchange, saleUpdatedQueue: Queue): Binding {
		return BindingBuilder.bind(saleUpdatedQueue).to(saleUpdatedFanout)
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

	@Component
	internal class DataPreLoader : CommandLineRunner {
		@Autowired
		var newsRepo: NewsRepository? = null

		override fun run(vararg args: String) {
			newsRepo!!.save(News(
					sale = 1,
					sellerName = "user name",
					bookTitle = "JavaScript: The Definitive Guide",
					bookPrice = 300,
					bookCondition = "Visibly used"
			))

			newsRepo!!.save(News(
					sale = 2,
					sellerName = "user name",
					bookTitle = "JavaScript: The Good Parts",
					bookPrice = 1000,
					bookCondition = "New"
			))

			newsRepo!!.save(News(
					sale = 3,
					sellerName = "admin name",
					bookTitle = "JavaScript: The Definitive Guide",
					bookPrice = 700,
					bookCondition = "Barely used"
			))

			newsRepo!!.save(News(
					sale = 4,
					sellerName = "admin name",
					bookTitle = "Clean Code",
					bookPrice = 0,
					bookCondition = "Poor"
			))
		}
	}
}