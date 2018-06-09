package no.gardos.gateway.model

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
		@Autowired
		private val userCrud: UserRepository,
		@Autowired
		private val passwordEncoder: PasswordEncoder
) {
	fun createUser(username: String, password: String, roles: Set<String> = setOf()): Boolean {
		try {
			val hashedPassword = passwordEncoder.encode(password)

			if (userCrud.exists(username)) return false

			val user = User(username, hashedPassword, roles.map { "ROLE_$it" }.toSet())
			userCrud.save(user)

			return true
		} catch (e: Exception) {
			return false
		}
	}
}