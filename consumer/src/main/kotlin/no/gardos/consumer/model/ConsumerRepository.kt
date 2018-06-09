package no.gardos.consumer.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ConsumerRepository : CrudRepository<Consumer, Long>