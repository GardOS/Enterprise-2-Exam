package no.exam.news.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableWebSecurity
class NewsSecurityConfig : WebSecurityConfigurerAdapter() {

	override fun configure(http: HttpSecurity) {
		http.httpBasic()
				.and()
				.authorizeRequests()
				.antMatchers(HttpMethod.GET, "/**").permitAll()
				.anyRequest().denyAll()
				.and()
				.csrf().disable()
	}
}