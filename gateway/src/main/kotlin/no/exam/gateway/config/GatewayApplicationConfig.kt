package no.exam.gateway.config

import com.netflix.config.ConfigurationManager
import no.exam.gateway.model.AuthUser
import no.exam.gateway.model.AuthUserRepository
import no.exam.gateway.model.AuthUserService
import org.springframework.amqp.core.FanoutExchange
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.cloud.netflix.zuul.EnableZuulProxy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@ComponentScan
@EnableZuulProxy
@EnableSwagger2
class GatewayApplicationConfig {

	init {
		val conf = ConfigurationManager.getConfigInstance()
		conf.setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", 5000)
	}

	@Bean
	fun passwordEncoder(): PasswordEncoder {
		return BCryptPasswordEncoder()
	}

	//User created MQ message
	@Bean
	fun fanout(): FanoutExchange {
		return FanoutExchange("user-created")
	}
}

@Component
internal class DataPreLoader : CommandLineRunner {
	@Autowired
	var authUserRepo: AuthUserRepository? = null
	@Autowired
	var passwordEncoder: PasswordEncoder? = null
	@Autowired
	var authUserService: AuthUserService? = null

	override fun run(vararg args: String) {
		val adminUsername = "admin"
		val adminPassword = passwordEncoder!!.encode("pwd")
		val adminRoles = hashSetOf("ROLE_USER", "ROLE_ADMIN", "ROLE_ACTUATOR")
		authUserService?.createUser(adminUsername, adminPassword, adminRoles)
		authUserRepo!!.save(AuthUser(adminUsername, adminPassword, adminRoles))

		val defaultUsername = "user"
		val defaultPassword = passwordEncoder!!.encode("pwd")
		val defaultRoles = hashSetOf("ROLE_USER")
		authUserService?.createUser(defaultUsername, defaultPassword, defaultRoles)
		authUserRepo!!.save(AuthUser(defaultUsername, defaultPassword, defaultRoles))
	}
}