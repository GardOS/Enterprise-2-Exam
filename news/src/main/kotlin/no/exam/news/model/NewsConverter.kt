package no.exam.news.model

import no.exam.schema.NewsDto

class NewsConverter {
	companion object {
		fun transform(news: News): NewsDto {
			return NewsDto(
					sale = news.sale,
					sellerName = news.sellerName,
					bookTitle = news.bookTitle,
					bookPrice = news.bookPrice,
					bookCondition = news.bookCondition
			)
		}

		fun transform(news: Iterable<News>): List<NewsDto> {
			return news.map { transform(it) }
		}
	}
}