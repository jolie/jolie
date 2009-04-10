type ConnectionInfo:void {
	.driver:string
	.host:string
	.port?:int
	.database:string
	.username:string
	.password:string
	.checkConnection?:int
}

type QueryResult:void {
	.row[0,*]:void { ? }
}

interface DatabaseInterface {
RequestResponse:
	connect(ConnectionInfo)(void) throws ConnectionError InvalidDriver,
	query(string)(QueryResult) throws SQLException ConnectionError,
	update(string)(int) throws SQLException ConnectionError
}

outputPort Database {
Interfaces: DatabaseInterface
}

embedded {
Java:
	"joliex.db.DatabaseService" in Database
}
