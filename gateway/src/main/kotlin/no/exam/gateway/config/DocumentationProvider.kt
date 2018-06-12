package no.exam.gateway.config

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger.web.SwaggerResource
import springfox.documentation.swagger.web.SwaggerResourcesProvider
import springfox.documentation.swagger.web.UiConfiguration
import java.util.*

//https://piotrminkowski.wordpress.com/2017/04/14/microservices-api-documentation-with-swagger2/
@Component
@Primary
@EnableAutoConfiguration
class DocumentationProvider : SwaggerResourcesProvider {

	@Bean
	internal fun swaggerUiConfig(): UiConfiguration {
		return UiConfiguration("validatorUrl", "list", "alpha", "schema",
				UiConfiguration.Constants.DEFAULT_SUBMIT_METHODS, false, true, 60000L)
	}

	@Bean
	fun swaggerApi(): Docket {
		return Docket(DocumentationType.SWAGGER_2)
				.apiInfo(apiInfo())
				.select()
				.paths(PathSelectors.any())
				.apis(RequestHandlerSelectors.basePackage("no.exam.gateway"))
				.build()
	}

	private fun apiInfo(): ApiInfo {
		return ApiInfoBuilder()
				.title("REST API for authentication")
				.version("1.0")
				.build()
	}

	override fun get(): List<SwaggerResource> {
		val resources = ArrayList<SwaggerResource>()
		resources.add(swaggerResource("gateway", "/v2/api-docs", "2.0"))
		resources.add(swaggerResource("book-server", "/book-server/v2/api-docs", "2.0"))
		resources.add(swaggerResource("sale-server", "/sale-server/v2/api-docs", "2.0"))
		resources.add(swaggerResource("user-server", "/user-server/v2/api-docs", "2.0"))
		resources.add(swaggerResource("news-server", "/news-server/v2/api-docs", "2.0"))
		return resources
	}

	private fun swaggerResource(name: String, location: String, version: String): SwaggerResource {
		val swaggerResource = SwaggerResource()
		swaggerResource.name = name
		swaggerResource.location = location
		swaggerResource.swaggerVersion = version
		return swaggerResource
	}
}