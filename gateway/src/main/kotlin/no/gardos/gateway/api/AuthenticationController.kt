package no.gardos.gateway.api

import io.swagger.annotations.Api
import no.gardos.gateway.model.UserService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@Api(description = "API for authentication.")
@RestController
@Validated
class AuthenticationController(
		private val service: UserService,
		private val authenticationManager: AuthenticationManager,
		private val userDetailsService: UserDetailsService
) {
	@RequestMapping("/user")
	fun getUser(user: Principal): ResponseEntity<Map<String, Any>> {
		val map = mutableMapOf<String, Any>()
		map["name"] = user.name
		map["roles"] = AuthorityUtils.authorityListToSet((user as Authentication).authorities)
		return ResponseEntity.ok(map)
	}

	@PostMapping(path = ["/signIn"], consumes = [(MediaType.APPLICATION_FORM_URLENCODED_VALUE)])
	fun signIn(@ModelAttribute(name = "username") username: String,
	           @ModelAttribute(name = "password") password: String
	): ResponseEntity<Void> {
		val registered = service.createUser(username, password, setOf("USER"))

		if (!registered) {
			return ResponseEntity.status(400).build()
		}

		val userDetails = userDetailsService.loadUserByUsername(username)
		val token = UsernamePasswordAuthenticationToken(userDetails, password, userDetails.authorities)

		authenticationManager.authenticate(token)

		if (token.isAuthenticated) {
			SecurityContextHolder.getContext().authentication = token
		}

		return ResponseEntity.status(204).build()
	}
}