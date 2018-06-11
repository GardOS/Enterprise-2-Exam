package no.exam.book.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.validation.constraints.NotNull

@Entity
class Book(
		@get:Id @get:GeneratedValue
		var id: Long? = null,
		@get:NotNull
		var title: String? = null,
		@get:NotNull
		var author: String? = null,
		@get:NotNull
		var condition: String? = null
)