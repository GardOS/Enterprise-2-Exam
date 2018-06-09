package no.gardos.schema

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.io.Serializable

@ApiModel("DTO for Producer")
data class ProducerDto(
		@ApiModelProperty("Name")
		var name: String? = null,

		@ApiModelProperty("Id")
		var id: Long? = null
) : Serializable