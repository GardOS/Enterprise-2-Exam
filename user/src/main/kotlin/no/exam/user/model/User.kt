package no.exam.user.model

import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id
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
		@get:ElementCollection(targetClass = Long::class)
		var sales: List<Long>? = null
)