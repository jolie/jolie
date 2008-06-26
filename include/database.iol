outputPort Database {
SolicitResponse:
	connect, query, update
}

embedded {
Java:
	"joliex.db.DatabaseService" in Database
}

