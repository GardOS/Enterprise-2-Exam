package no.gardos.sale.model

import no.gardos.schema.SaleDto

class SaleConverter {
	companion object {
		fun transform(sale: Sale): SaleDto {
			return SaleDto(
					id = sale.id,
					user = sale.user,
					book = sale.book,
					price = sale.price
			)
		}

		fun transform(categories: Iterable<Sale>): List<SaleDto> {
			return categories.map { transform(it) }
		}
	}
}