package no.exam.news.model

import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.NotNull

@Entity
class News(
		@get:Id
		var sale: Long? = null,
		@get:NotNull
		var sellerName: String? = null,
		@get:NotNull
		var bookTitle: String? = null,
		@get:NotNull
		var bookPrice: Int? = null,
		@get:NotNull
		var bookCondition: String? = null
)