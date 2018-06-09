package no.gardos.gateway

import com.netflix.config.ConfigurationManager
import no.gardos.gateway.model.User
import no.gardos.gateway.model.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.zuul.EnableZuulProxy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import springfox.documentation.swagger.web.UiConfiguration
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication
@ComponentScan
@EnableZuulProxy
@EnableSwagger2
class GatewayApplication {

	init {
		val conf = ConfigurationManager.getConfigInstance()
		conf.setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", 5000)
	}

	@Bean
	internal fun swaggerUiConfig(): UiConfiguration {
		return UiConfiguration("validatorUrl", "list", "alpha", "schema",
				UiConfiguration.Constants.DEFAULT_SUBMIT_METHODS, false, true, 60000L)
	}

	@Bean
	fun passwordEncoder(): PasswordEncoder {
		return BCryptPasswordEncoder()
	}
}

fun main(args: Array<String>) {
	SpringApplication.run(GatewayApplication::class.java, *args)
}

@Component
internal class CommandLineAppStartupRunner : CommandLineRunner {
	@Autowired
	var userRepository: UserRepository? = null
	@Autowired
	var passwordEncoder: PasswordEncoder? = null

	override fun run(vararg args: String) {
		val roles = hashSetOf("ROLE_USER", "ROLE_ADMIN", "ROLE_ACTUATOR")
		userRepository!!.save(User("admin", passwordEncoder!!.encode("pwd"), roles))
	}
}