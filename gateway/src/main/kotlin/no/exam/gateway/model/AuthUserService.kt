package no.exam.gateway.model

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthUserService(
		@Autowired
		private val authUserRepo: AuthUserRepository,
		@Autowired
		private val passwordEncoder: PasswordEncoder
) {
	fun createUser(username: String, password: String, roles: Set<String> = setOf()): Boolean {
		try {
			val hashedPassword = passwordEncoder.encode(password)

			if (authUserRepo.exists(username)) return false

			val authUser = AuthUser(username, hashedPassword, roles.map { "ROLE_$it" }.toSet())
			authUserRepo.save(authUser)

			return true
		} catch (e: Exception) {
			return false
		}
	}

	fun getUser(username: String, password: String): AuthUser? {
		try {
			val authUser = authUserRepo.findOne(username)
			if (!passwordEncoder.matches(password, authUser.password)) {
				return null
			}
			return authUser
		} catch (e: Exception) {
			return null
		}
	}
}