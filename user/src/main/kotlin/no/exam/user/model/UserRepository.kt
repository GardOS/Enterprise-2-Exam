package no.exam.user.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CrudRepository<User, String> {
	fun deleteBySales(id: Long)
}