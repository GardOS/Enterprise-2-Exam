package no.exam.book.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableWebSecurity
class BookSecurityConfig : WebSecurityConfigurerAdapter() {

	override fun configure(http: HttpSecurity) {
		http.httpBasic()
				.and()
				.authorizeRequests()
				.antMatchers(HttpMethod.GET, "/**").permitAll()
				.antMatchers(HttpMethod.POST, "/**").hasRole("USER")
				.antMatchers(HttpMethod.PUT, "/**").hasRole("ADMIN")
				.antMatchers(HttpMethod.PATCH, "/**").hasRole("ADMIN")
				.antMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")
				.anyRequest().denyAll()
				.and()
				.csrf().disable()
	}
}