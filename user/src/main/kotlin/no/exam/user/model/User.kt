package no.exam.user.model

import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
class User(
		@get:Id @get:Column(unique = true)
		var username: String? = null,
		@get:NotNull
		var name: String? = null,
		@get:NotNull
		var email: String? = null,
		@get:NotNull
		@get:ElementCollection(targetClass = Long::class, fetch = FetchType.EAGER)
		var sales: MutableList<Long>? = null
)