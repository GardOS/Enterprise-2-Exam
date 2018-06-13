package no.exam.sale.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.exam.sale.model.Sale
import no.exam.sale.model.SaleRepository
import org.springframework.amqp.core.FanoutExchange
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.cloud.netflix.ribbon.RibbonClient
import org.springframework.cloud.netflix.ribbon.RibbonClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
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
@EnableEurekaClient
@EnableSwagger2
@RibbonClients(value = [
	RibbonClient(name = "book-server"),
	RibbonClient(name = "seller-server")
])
class SaleApplicationConfig {

	@Bean
	fun swaggerApi(): Docket {
		return Docket(DocumentationType.SWAGGER_2)
				.apiInfo(apiInfo())
				.select()
				.paths(PathSelectors.any())
				.apis(RequestHandlerSelectors.basePackage("no.exam.sale"))
				.build()
	}

	private fun apiInfo(): ApiInfo {
		return ApiInfoBuilder()
				.title("REST API for interacting with the sale")
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
	fun loadBalancedRestTemplate(): RestTemplate {
		return RestTemplate()
	}

	//Although bad practice we use this bean for testing purposes
	//The reason for this is because of complications of mocking a @LoadBalanced bean
	//There are probably ways to disable ribbon, or inject your own test beans, but its outside scope for this exam
	@Bean
	@Profile("test")
	fun restTemplate(): RestTemplate {
		return RestTemplate()
	}

	//Sale created MQ message
	@Bean
	fun saleCreatedFanout(): FanoutExchange {
		return FanoutExchange("sale-created")
	}

	//Sale updated MQ message
	@Bean
	fun saleUpdatedFanout(): FanoutExchange {
		return FanoutExchange("sale-updated")
	}

	//Sale deleted MQ message
	@Bean
	fun saleDeletedFanout(): FanoutExchange {
		return FanoutExchange("sale-deleted")
	}
}

@Component
internal class DataPreLoader : CommandLineRunner {
	@Autowired
	var saleRepo: SaleRepository? = null

	override fun run(vararg args: String) {
		saleRepo!!.save(Sale(
				seller = "seller",
				book = 1,
				price = 300,
				condition = "Visibly used"
		))

		saleRepo!!.save(Sale(
				seller = "seller",
				book = 2,
				price = 1000,
				condition = "New"
		))

		saleRepo!!.save(Sale(
				seller = "admin",
				book = 1,
				price = 700,
				condition = "Barely used"
		))

		saleRepo!!.save(Sale(
				seller = "admin",
				book = 3,
				price = 0,
				condition = "Poor"
		))
	}
}