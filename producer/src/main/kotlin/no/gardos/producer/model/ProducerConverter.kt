package no.gardos.producer.model

import no.gardos.schema.ProducerDto

class ProducerConverter {
	companion object {
		fun transform(producer: Producer): ProducerDto {
			return ProducerDto(
					name = producer.name,
					id = producer.id
			)
		}

		fun transform(categories: Iterable<Producer>): List<ProducerDto> {
			return categories.map { transform(it) }
		}
	}
}