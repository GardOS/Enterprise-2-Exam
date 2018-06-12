package no.exam.schema

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.io.Serializable

@ApiModel("DTO for User")
data class UserDto(
		@ApiModelProperty("Username of the user")
		var username: String? = null,

		@ApiModelProperty("Name of the user")
		var name: String? = null,

		@ApiModelProperty("Email address of the user")
		var email: String? = null,

		@ApiModelProperty("Current sales the user has created")
		var sales: MutableList<Long>? = null
) : Serializable