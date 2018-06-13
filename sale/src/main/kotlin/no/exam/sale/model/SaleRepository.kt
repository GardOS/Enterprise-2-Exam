package no.exam.sale.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SaleRepository : CrudRepository<Sale, Long> {
	fun findByBook(book: Long): List<Sale>
	fun findBySeller(seller: String): List<Sale>
}