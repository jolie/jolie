interface DatabaseInterface {
RequestResponse:
	connect, query, update
}

outputPort Database {
Interfaces: DatabaseInterface
}

embedded {
Java:
	"joliex.db.DatabaseService" in Database
}
