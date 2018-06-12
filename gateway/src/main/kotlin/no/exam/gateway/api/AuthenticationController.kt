package no.exam.gateway.api

import io.swagger.annotations.Api
import no.exam.gateway.model.AuthUserService
import no.exam.schema.UserDto
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.security.Principal
import javax.servlet.http.HttpSession

@Api(description = "API for authentication")
@RestController
@Validated
class AuthenticationController(
		private val authUserService: AuthUserService,
		private val authenticationManager: AuthenticationManager,
		private val userDetailsService: UserDetailsService
) {
	@Autowired
	private lateinit var rabbitTemplate: RabbitTemplate

	@Autowired
	private lateinit var fanout: FanoutExchange

	@RequestMapping("/authUser")
	fun getAuthUser(principal: Principal): ResponseEntity<Map<String, Any>> {
		val map = mutableMapOf<String, Any>()
		map["name"] = principal.name
		map["roles"] = AuthorityUtils.authorityListToSet((principal as Authentication).authorities)
		return ResponseEntity.ok(map)
	}

	@PostMapping(path = ["/register"], consumes = [(MediaType.APPLICATION_FORM_URLENCODED_VALUE)])
	fun register(
			@ModelAttribute(name = "username") username: String,
			@ModelAttribute(name = "password") password: String,
			@ModelAttribute(name = "name") name: String,
			@ModelAttribute(name = "email") email: String
	): ResponseEntity<Void> {
		val registered = authUserService.createUser(username, password, setOf("USER"))

		if (!registered) {
			return ResponseEntity.status(400).build()
		}

		//TODO: validation?
		val user = UserDto(
				username = username,
				name = name,
				email = email
		)

		rabbitTemplate.convertAndSend(fanout.name, "", user)

		val userDetails = userDetailsService.loadUserByUsername(username)
		val token = UsernamePasswordAuthenticationToken(userDetails, password, userDetails.authorities)

		authenticationManager.authenticate(token)

		if (token.isAuthenticated) {
			SecurityContextHolder.getContext().authentication = token
		}

		return ResponseEntity.status(204).build()
	}

	@GetMapping(path = ["/logout"])
	fun logout(session: HttpSession): ResponseEntity<Void> {
		session.invalidate()
		return ResponseEntity.status(204).build()
	}

	@PostMapping(path = ["/login"], consumes = [(MediaType.APPLICATION_FORM_URLENCODED_VALUE)])
	fun login(
			@ModelAttribute(name = "username") username: String,
			@ModelAttribute(name = "password") password: String
	): ResponseEntity<Void> {
		authUserService.getUser(username, password) ?: return ResponseEntity.status(401).build()

		val userDetails = userDetailsService.loadUserByUsername(username)
		val token = UsernamePasswordAuthenticationToken(userDetails, password, userDetails.authorities)

		authenticationManager.authenticate(token)

		if (token.isAuthenticated) {
			SecurityContextHolder.getContext().authentication = token
		}

		return ResponseEntity.status(204).build()
	}
}