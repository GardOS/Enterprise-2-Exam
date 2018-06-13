package no.exam.sale.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.validation.constraints.NotNull

@Entity
class Sale(
		@get:Id @get:GeneratedValue
		var id: Long? = null,
		@get:NotNull
		var seller: String? = null,
		@get:NotNull
		var book: Long? = null,
		@get:NotNull
		var price: Int? = null,
		@get:NotNull
		var condition: String? = null
)