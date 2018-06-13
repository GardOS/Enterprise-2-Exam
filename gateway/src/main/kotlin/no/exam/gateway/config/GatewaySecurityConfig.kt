package no.exam.gateway.config

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
class GatewaySecurityConfig(
		private val dataSource: DataSource,
		private val passwordEncoder: PasswordEncoder
) : WebSecurityConfigurerAdapter() {

	@Bean
	override fun userDetailsServiceBean(): UserDetailsService {
		return super.userDetailsServiceBean()
	}

	override fun configure(http: HttpSecurity) {
		http.antMatcher("/swagger-ui.html") //Avoid httpBasic popup login
				.httpBasic()
				.and()
				.logout()
				.and()
				.authorizeRequests()
				//Swagger-ui
				.antMatchers("/swagger-resources/**", "/webjars/**", "/swagger-ui.html", "/v2/api-docs").permitAll()
				//Actuator
				.antMatchers("/health").permitAll()
				.antMatchers("/trace").authenticated()
				//Auth
				.antMatchers("/login").permitAll()
				.antMatchers("/logout").permitAll()
				.antMatchers("/register").permitAll()
				.antMatchers("/authUser").authenticated()
				//Book
				.antMatchers(HttpMethod.GET, "/book-server/**").permitAll()
				.antMatchers("/book-server/**").authenticated()
				//Sale
				.antMatchers(HttpMethod.GET, "/sale-server/**").permitAll()
				.antMatchers("/sale-server/**").authenticated()
				//Seller
				.antMatchers(HttpMethod.GET, "/seller-server/**").permitAll()
				//News
				.antMatchers(HttpMethod.GET, "/news-server/**").permitAll()
				.anyRequest().denyAll()
				.and()
				.csrf()
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
	}

	override fun configure(auth: AuthenticationManagerBuilder) {
		auth.jdbcAuthentication()
				.dataSource(dataSource)
				.usersByUsernameQuery("SELECT username, password, enabled " +
						"FROM auth_user WHERE username=?")
				.authoritiesByUsernameQuery(
						"SELECT x.username, y.roles FROM auth_user x, auth_user_roles y " +
								"WHERE x.username=? and y.auth_user_username=x.username")
				.passwordEncoder(passwordEncoder)
	}
}