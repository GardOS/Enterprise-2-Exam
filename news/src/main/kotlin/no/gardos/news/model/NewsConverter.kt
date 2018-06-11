package no.gardos.news.model

import no.gardos.schema.NewsDto

class NewsConverter {
	companion object {
		fun transform(news: News): NewsDto {
			return NewsDto(
					id = news.id,
					sale = news.sale,
					sellerName = news.sellerName,
					bookTitle = news.bookTitle,
					bookPrice = news.bookPrice
			)
		}

		fun transform(categories: Iterable<News>): List<NewsDto> {
			return categories.map { transform(it) }
		}
	}
}