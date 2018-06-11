package no.gardos.schema

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.io.Serializable

@ApiModel("DTO for News")
data class NewsDto(
		@ApiModelProperty("Id of the news")
		var id: Long? = null,

		@ApiModelProperty("Reference to sale that triggered the news")
		var sale: Long? = null,

		@ApiModelProperty("Name of the user that is selling the book")
		var sellerName: String? = null,

		@ApiModelProperty("Title of the book")
		var bookTitle: String? = null,

		@ApiModelProperty("Price of the book")
		var bookPrice: Int? = null
) : Serializable