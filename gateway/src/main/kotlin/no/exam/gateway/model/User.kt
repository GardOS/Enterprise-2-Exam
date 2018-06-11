package no.exam.gateway.model

import org.hibernate.validator.constraints.NotBlank
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.NotNull

@Entity
class User(
		@get:Id
		@get:NotBlank
		var username: String?,

		@get:NotBlank
		var password: String?,

		@get:ElementCollection
		@get:NotNull
		var roles: Set<String>? = setOf(),

		@get:NotNull
		var enabled: Boolean? = true
)