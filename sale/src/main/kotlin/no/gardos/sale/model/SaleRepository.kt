package no.gardos.sale.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SaleRepository : CrudRepository<Sale, Long>