package no.exam.gateway.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface AuthUserRepository : CrudRepository<AuthUser, String>, AuthUserRepositoryCustom

@Transactional
interface AuthUserRepositoryCustom