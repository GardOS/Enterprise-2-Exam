package no.gardos.producer.model

import org.jetbrains.annotations.NotNull
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class Producer(
		@get:NotNull
		var name: String? = null,
		@get:Id @get:GeneratedValue
		var id: Long? = null
)