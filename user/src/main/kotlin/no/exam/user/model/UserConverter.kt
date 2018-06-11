package no.exam.user.model

import no.exam.schema.UserDto

class UserConverter {
	companion object {
		fun transform(user: User): UserDto {
			return UserDto(
					username = user.username,
					name = user.name,
					email = user.email,
					sales = user.sales
			)
		}

		fun transform(users: Iterable<User>): List<UserDto> {
			return users.map { transform(it) }
		}
	}
}