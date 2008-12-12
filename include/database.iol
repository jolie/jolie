interface DatabaseInterface {
RequestResponse:
	connect throws ConnectionError InvalidDriver,
	query throws SQLException ConnectionError,
	update throws SQLException ConnectionError
}

outputPort Database {
Interfaces: DatabaseInterface
}

embedded {
Java:
	"joliex.db.DatabaseService" in Database
}
