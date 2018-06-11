package no.exam.schema

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.io.Serializable

@ApiModel("DTO for Sale")
data class SaleDto(
		@ApiModelProperty("Id of the sale")
		var id: Long? = null,

		@ApiModelProperty("Reference to user that is selling")
		var user: String? = null,

		@ApiModelProperty("Reference to book that is being sold")
		var book: Long? = null,

		@ApiModelProperty("Price of the book")
		var price: Int? = null
) : Serializable