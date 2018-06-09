package no.gardos.producer.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ProducerRepository : CrudRepository<Producer, Long>