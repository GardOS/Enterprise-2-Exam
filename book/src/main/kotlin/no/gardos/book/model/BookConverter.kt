package no.gardos.book.model

import no.gardos.schema.BookDto

class BookConverter {
	companion object {
		fun transform(book: Book): BookDto {
			return BookDto(
					id = book.id,
					title = book.title,
					author = book.author,
					condition = book.condition
			)
		}

		fun transform(categories: Iterable<Book>): List<BookDto> {
			return categories.map { transform(it) }
		}
	}
}