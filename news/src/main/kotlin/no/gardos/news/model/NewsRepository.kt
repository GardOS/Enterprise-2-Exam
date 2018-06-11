package no.gardos.news.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface NewsRepository : CrudRepository<News, Long>