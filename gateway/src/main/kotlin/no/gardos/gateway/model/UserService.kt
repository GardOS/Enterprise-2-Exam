package no.gardos.gateway.model

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
		@Autowired
		private val userRepo: UserRepository,
		@Autowired
		private val passwordEncoder: PasswordEncoder
) {
	fun createUser(username: String, password: String, roles: Set<String> = setOf()): Boolean {
		try {
			val hashedPassword = passwordEncoder.encode(password)

			if (userRepo.exists(username)) return false

			val user = User(username, hashedPassword, roles.map { "ROLE_$it" }.toSet())
			userRepo.save(user)

			return true
		} catch (e: Exception) {
			return false
		}
	}

	fun getUser(username: String, password: String): User? {
		try {
			val user = userRepo.findOne(username)
			if (!passwordEncoder.matches(password, user.password)) {
				return null
			}
			return user
		} catch (e: Exception) {
			return null
		}
	}
}