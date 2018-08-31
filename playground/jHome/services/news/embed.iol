include "news.iol"

outputPort News {
Interfaces: NewsInterface
}

embedded {
Jolie:
	"services/news/main.ol" in News
}