package no.exam.sale.model

import no.exam.schema.SaleDto

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

		fun transform(sales: Iterable<Sale>): List<SaleDto> {
			return sales.map { transform(it) }
		}
	}
}