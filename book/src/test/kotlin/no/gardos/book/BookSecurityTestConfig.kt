package no.gardos.book

import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@Configuration
@EnableWebSecurity
@Order(1)
class BookSecurityTestConfig : BookSecurityConfig() {
	override fun configure(auth: AuthenticationManagerBuilder) {
		auth.inMemoryAuthentication()
				.withUser("testUser").password("pwd").roles("USER").and()
				.withUser("testAdmin").password("pwd").roles("USER", "ADMIN")
	}
}
