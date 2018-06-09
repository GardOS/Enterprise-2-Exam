package no.gardos.gateway.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface UserRepository : CrudRepository<User, String>, UserRepositoryCustom

@Transactional
interface UserRepositoryCustom