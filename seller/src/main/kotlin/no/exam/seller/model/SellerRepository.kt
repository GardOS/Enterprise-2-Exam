package no.exam.seller.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SellerRepository : CrudRepository<Seller, String>