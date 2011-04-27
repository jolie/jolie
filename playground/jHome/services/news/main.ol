include "news.iol"
include "console.iol"

execution { concurrent }

inputPort NewsInput {
Location: "local"
Interfaces: NewsInterface
}

main
{
	getNewsList()( response ) {
		response.item[i++] = "News 1";
		response.item[i++] = "News 2"
	}
}