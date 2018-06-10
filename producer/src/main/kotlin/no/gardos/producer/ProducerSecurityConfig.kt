package no.gardos.producer

import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableWebSecurity
class ProducerSecurityConfig : WebSecurityConfigurerAdapter() {

	override fun configure(http: HttpSecurity) {
		http.httpBasic()
				.and()
				.authorizeRequests()
				.antMatchers(HttpMethod.GET, "/**").permitAll()
				.anyRequest().authenticated()
				.and()
				.csrf().disable()
	}
}