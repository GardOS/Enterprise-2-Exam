package no.gardos.book.model

import org.jetbrains.annotations.NotNull
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

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