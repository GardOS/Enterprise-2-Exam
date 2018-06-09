package no.gardos.gateway

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import javax.sql.DataSource


@Configuration
@EnableWebSecurity
class WebSecurityConfig(
		private val dataSource: DataSource,
		private val passwordEncoder: PasswordEncoder
) : WebSecurityConfigurerAdapter() {

	@Bean
	override fun userDetailsServiceBean(): UserDetailsService {
		return super.userDetailsServiceBean()
	}

	override fun configure(http: HttpSecurity) {
		http.httpBasic()
				.and()
				.logout()
				.and()
				.authorizeRequests()
				//Swagger-ui
				.antMatchers("/swagger-resources/**", "/webjars/**", "/swagger-ui.html").permitAll()
				//Actuator
				.antMatchers("/health").permitAll()
				.antMatchers("/trace").authenticated()
				//Auth
				.antMatchers("/signIn").permitAll()
				.antMatchers("/user").authenticated()
				//Producer
				.antMatchers(HttpMethod.GET, "/producer-server/**").permitAll() //FIXME
				.antMatchers("/producer-server/**").authenticated() //FIXME
				//Consumer
				.antMatchers(HttpMethod.GET, "/consumer-server/**").permitAll() //FIXME
				.antMatchers("/consumer-server/**").authenticated() //FIXME
				.anyRequest().denyAll()
				.and()
				.csrf()
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
	}

	override fun configure(auth: AuthenticationManagerBuilder) {
		auth.jdbcAuthentication()
				.dataSource(dataSource)
				.usersByUsernameQuery("SELECT username, password, enabled FROM user WHERE username=?")
				.authoritiesByUsernameQuery(
						"SELECT x.username, y.roles FROM user x, user_roles y " +
								"WHERE x.username=? and y.user_username=x.username")
				.passwordEncoder(passwordEncoder)
	}
}