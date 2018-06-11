package no.exam.sale.model

import org.jetbrains.annotations.NotNull
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class Sale(
		@get:Id @get:GeneratedValue
		var id: Long? = null,
		@get:NotNull
		var user: String? = null,
		@get:NotNull
		var book: Long? = null,
		@get:NotNull
		var price: Int? = null

)