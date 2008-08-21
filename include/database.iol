outputPort Database {
RequestResponse:
	connect, query, update
}

embedded {
Java:
	"joliex.db.DatabaseService" in Database
}

