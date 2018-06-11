package no.exam.news.model

import org.jetbrains.annotations.NotNull
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class News(
		@get:Id @get:GeneratedValue
		var id: Long? = null,
		@get:NotNull
		var sale: Long? = null,
		@get:NotNull
		var sellerName: String? = null,
		@get:NotNull
		var bookTitle: String? = null,
		@get:NotNull
		var bookPrice: Int? = null
)