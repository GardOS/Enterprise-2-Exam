# Exam

## NOTES:

Test format: Method_ExpectedResult

e2e crash ref:
https://github.com/docker/for-win/issues/2007
https://github.com/docker/for-win/issues/1723
https://github.com/docker/for-win/issues/1563
https://www.reddit.com/r/docker/comments/815l9n/docker_for_windows_wont_start_if_razer_synapse_3/

#### Book:
* Edition: string because there are so many different ways to write it. Can be null since user might not 
know/understand/care
* Put: Since Id is auto-increment have to go with some alternative way.

#### News:
* Could have only references on the news, but chose name/price instead so that its not dependent on other services when
 presenting news. This however makes it too loosely coupled with book, as there currently is no functionality of 
 updating the news if an admin updates a name.
* When updated the order is not changed
