package no.gardos.consumer.model

import no.gardos.schema.ConsumerDto

class ConsumerConverter {
	companion object {
		fun transform(consumer: Consumer): ConsumerDto {
			return ConsumerDto(
					name = consumer.name,
					id = consumer.id
			)
		}

		fun transform(categories: Iterable<Consumer>): List<ConsumerDto> {
			return categories.map { transform(it) }
		}
	}
}