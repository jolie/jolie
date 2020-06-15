/*
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>         
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                      
 *                                                                        
 *   This program is free software; you can redistribute it and/or modify 
 *   it under the terms of the GNU Library General Public License as      
 *   published by the Free Software Foundation; either version 2 of the   
 *   License, or (at your option) any later version.                      
 *                                                                        
 *   This program is distributed in the hope that it will be useful,      
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of       
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 *   GNU General Public License for more details.                         
 *                                                                        
 *   You should have received a copy of the GNU Library General Public    
 *   License along with this program; if not, write to the                
 *   Free Software Foundation, Inc.,                                      
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.            
 *                                                                        
 *   For details about the authors of this software, see the AUTHORS file.
 */

type ConnectionInfo:void {
	.driver:string // http://docs.jolie-lang.org/#!documentation/databases/databases.html
	.host:string
	.port?:int
	.database:string
	.username:string
	.password:string
	.attributes?:string // further semicolon-separated JDBC connection string parameters
	.checkConnection?:int // if true (> 0) check connection before each DB command (default: false (0))
	.toLowerCase?: bool // lowercase attribute names
	.toUpperCase?: bool // uppercase attribute names
	.encoding?: string
}

type QueryResult:void {
	.row[0,*]:void { ? }
}

type TransactionQueryResult:int {
	.row[0,*]:void { ? }
}

type DatabaseTransactionRequest:void {
	.statement[1,*]:string { ? }
}

type DatabaseTransactionResult:void {
	.result[0,*]:TransactionQueryResult
}

type QueryRequest:string { ? }

type UpdateRequest:string { ? }

interface DatabaseInterface {
RequestResponse:
	/**!
	 * Connects to a database and eventually closes a previous connection
	 *
	 * Example with HSQLDB:
	 * with ( connectionInfo ) {
	 *     .username = "sa";
	 *     .password = "";
	 *     .host = "";
	 *     .database = "file:weatherdb/weatherdb"; // "." for memory-only
	 *     .driver = "hsqldb_embedded"
	 * };
	 * connect@Database( connectionInfo )( void );
	 */
	connect(ConnectionInfo)(void) throws ConnectionError InvalidDriver DriverClassNotFound,
	/**!
	 * Explicitly closes a database connection
	 * Per default the close happens on reconnect or on termination of the
	 * Database service, eg. when the enclosing program finishes.
	 */
	close(void)(void),
	
	/**!
	 * Queries the database and returns a result set
	 *
	 * Example with SQL parameters:
	 * queryRequest =
	 *     "SELECT city, country, data FROM weather " +
	 *     "WHERE city=:city AND country=:country";
	 * queryRequest.city = City;
	 * queryRequest.country = Country;
	 * query@Database( queryRequest )( queryResponse );
	 *
	 * _template:
	 * Field _template allows for the definition of a specific output template.
	 * Assume, e.g., to have a table with the following columns:
	 * | col1 | col2 | col3 | col4 |
	 * If _template is not used the output will be rows with the following format:
	 * row
	 *  |-col1
	 *  |-col2
	 *  |-col3
	 *  |-col4
	 * Now let us suppose we would like to have the following structure for each row:
	 * row
	 *   |-mycol1			contains content of col1
	 *       |-mycol2		contains content of col2
	 * 	 |-mycol3		contains content of col3
	 *   |-mycol4			contains content of col4
	 *
	 * In order to achieve this, we can use field _template as it follows:
	 *   with( query_request._template ) {
	 *     .mycol1 = "col1";
	 *     .mycol1.mycol2 = "col2";
	 *     .mycol1.mycol2.mycol3 = "col3";
	 *     .mycol4 = "col4"
	 *   }
	 * _template does not currently support vectors.
	 */
	query(QueryRequest)(QueryResult) throws SQLException ConnectionError,
	/**!
	 * Updates the database and returns a single status code
	 *
	 * Example with SQL parameters:
	 * updateRequest =
	 *     "INSERT INTO weather(city, country, data) " +
	 *     "VALUES (:city, :country, :data)";
	 * updateRequest.city = City;
	 * updateRequest.country = Country;
	 * updateRequest.data = r;
	 * update@Database( updateRequest )( ret )
	 */
	update(UpdateRequest)(int) throws SQLException ConnectionError,
	/**!
	 * Checks the connection with the database. Throws ConnectionError if the connection is not functioning properly.
	 */
	checkConnection( void )( void ) throws ConnectionError,
	/**!
	 * Executes more than one database command in a single transaction
	 */
	executeTransaction(DatabaseTransactionRequest)(DatabaseTransactionResult) throws SQLException ConnectionError
}

outputPort Database {
Interfaces: DatabaseInterface
}

embedded {
Java:
	"joliex.db.DatabaseService" in Database
}
