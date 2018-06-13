package no.exam.sale.model

import no.exam.schema.SaleDto

class SaleConverter {
	companion object {
		fun transform(sale: Sale): SaleDto {
			return SaleDto(
					id = sale.id,
					seller = sale.seller,
					book = sale.book,
					price = sale.price,
					condition = sale.condition
			)
		}

		fun transform(sales: Iterable<Sale>): List<SaleDto> {
			return sales.map { transform(it) }
		}
	}
}