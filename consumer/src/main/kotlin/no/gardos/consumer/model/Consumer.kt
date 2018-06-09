package no.gardos.consumer.model

import org.jetbrains.annotations.NotNull
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class Consumer(
		@get:NotNull
		var name: String? = null,
		@get:Id @get:GeneratedValue
		var id: Long? = null
)