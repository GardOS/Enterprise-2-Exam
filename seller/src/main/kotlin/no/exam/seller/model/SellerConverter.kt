package no.exam.seller.model

import no.exam.schema.SellerDto

class SellerConverter {
	companion object {
		fun transform(seller: Seller): SellerDto {
			return SellerDto(
					username = seller.username,
					name = seller.name,
					email = seller.email,
					sales = seller.sales
			)
		}

		fun transform(users: Iterable<Seller>): List<SellerDto> {
			return users.map { transform(it) }
		}
	}
}