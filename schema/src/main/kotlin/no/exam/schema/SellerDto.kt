package no.exam.schema

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.io.Serializable

@ApiModel("DTO for Seller")
data class SellerDto(
		@ApiModelProperty("Username of the seller")
		var username: String? = null,

		@ApiModelProperty("Name of the seller")
		var name: String? = null,

		@ApiModelProperty("Email address of the seller")
		var email: String? = null,

		@ApiModelProperty("Current sales the seller has created")
		var sales: MutableList<Long>? = null
) : Serializable