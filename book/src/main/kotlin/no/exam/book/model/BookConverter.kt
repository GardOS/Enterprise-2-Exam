package no.exam.book.model

import no.exam.schema.BookDto

class BookConverter {
	companion object {
		fun transform(book: Book): BookDto {
			return BookDto(
					id = book.id,
					title = book.title,
					author = book.author,
					edition = book.edition
			)
		}

		fun transform(books: Iterable<Book>): List<BookDto> {
			return books.map { transform(it) }
		}
	}
}