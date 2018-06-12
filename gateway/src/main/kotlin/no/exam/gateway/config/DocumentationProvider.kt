package no.exam.gateway.config

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import springfox.documentation.swagger.web.SwaggerResource
import springfox.documentation.swagger.web.SwaggerResourcesProvider
import java.util.*

//https://piotrminkowski.wordpress.com/2017/04/14/microservices-api-documentation-with-swagger2/
@Component
@Primary
@EnableAutoConfiguration
class DocumentationProvider : SwaggerResourcesProvider {

	override fun get(): List<SwaggerResource> {
		val resources = ArrayList<SwaggerResource>()
		resources.add(swaggerResource("book-server", "/producer-server/v2/api-docs", "2.0"))
		resources.add(swaggerResource("sale-server", "/producer-server/v2/api-docs", "2.0"))
		resources.add(swaggerResource("user-server", "/producer-server/v2/api-docs", "2.0"))
		resources.add(swaggerResource("news-server", "/producer-server/v2/api-docs", "2.0"))
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