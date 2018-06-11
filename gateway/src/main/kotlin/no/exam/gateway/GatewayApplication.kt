package no.exam.gateway

import com.netflix.config.ConfigurationManager
import no.exam.gateway.model.AuthUser
import no.exam.gateway.model.AuthUserRepository
import no.exam.gateway.model.AuthUserService
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
	var authUserRepository: AuthUserRepository? = null
	@Autowired
	var passwordEncoder: PasswordEncoder? = null
	@Autowired
	var authUserService: AuthUserService? = null

	override fun run(vararg args: String) {
		val username = "admin"
		val password = passwordEncoder!!.encode("pwd")
		val roles = hashSetOf("ROLE_USER", "ROLE_ADMIN", "ROLE_ACTUATOR")
		authUserService?.createUser(username, password, roles)
		authUserRepository!!.save(AuthUser(username, password, roles))
	}
}