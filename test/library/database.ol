from ..test-unit import TestUnitInterface
from database import Database

service Main{
    inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

    embed Database as Database
    
    init {
        global.connection << {
			driver = "hsqldb_embedded"
			database = "."
			username = "jolie"
			password = "hs"
			host = ""
		}
    }

    define resetDatabase{
        connect@Database( global.connection )()

        // Setting transaction control to MVCC enables multiple transactions to happen on the same 
        update@Database("SET DATABASE TRANSACTION CONTROL MVCC;")()
        update@Database("CREATE TABLE IF NOT EXISTS testTable(id INTEGER, testString VARCHAR(50));")()
        update@Database("SET TABLE testTable SOURCE \"testTable.csv;fs=|\";" )()
        update@Database("DELETE FROM testTable WHERE true;")()
        update@Database("INSERT INTO testTable(id, testString) VALUES (1337, 'testUser');")()
    }

    main {
		test()() {
            resetDatabase

            /** Querying **/
            query@Database("SELECT * FROM testTable;")(queryResponse)
            if (#queryResponse.row != 1 || queryResponse.row[0].id != "1337"){
                throw( TestFailed, "Querying returned wrong result" )
            }
            resetDatabase

            /** Insertion **/
            update@Database("INSERT INTO testTable(id, testString) VALUES (42, 'NewTestUser');")(numberRowsAffected)
            if (numberRowsAffected != 1){
                throw( TestFailed, "Insering a new row affected the wrong number of rows" )
            }
            resetDatabase

            /** Updating **/
            update@Database("UPDATE testTable SET teststring = 'UpdatedUsername' where id = 1337;")(numberRowsAffected)
            query@Database("SELECT * FROM testTable;")(queryResponse)
            if (queryResponse.row[0].testString != "UpdatedUsername"){
                throw( TestFailed, "Updating an entry did not execute correctly" )
            }
            resetDatabase

            /** Executing a transaction using executeTransaction **/
            with (statements){
                .statement[0] = "INSERT INTO testTable(id, testString) VALUES (42, 'transactionUser');"
                .statement[1] = "DELETE FROM testTable WHERE id = 1337;"
            }
            executeTransaction@Database(statements)()
            query@Database("SELECT * FROM testTable;")(queryResponse)
            if (#queryResponse.row != 1 || queryResponse.row[0].id != 42){
                throw( TestFailed, "Executing a transaction using executeTransaction failed" )
            }
            resetDatabase

            /** Initialize transaction using beginTx **/
            beginTx@Database()(txHandle)
            if (!is_defined(txHandle)){
                throw( TestFailed, "Could not open a transaction using beginTx" )
            }
            rollbackTx@Database(txHandle)()
            resetDatabase

            /** Cannot execute on a non-existant transaction **/
            s << 
            {
                query = "SELECT * FROM testTable;"
                txHandle = -12
            }
            scope (ShouldThrow){
                install(TransactionException => {x = true}) // Setting x to true since something needs to happen here for the compiler to not complain
                query@Database(s)(queryResponse)
            }
            if (!is_defined(ShouldThrow.TransactionException)){
                throw( TestFailed, "Attempts to execute a transaction on a non-existant txHandle should not succeed!" )
            }
            resetDatabase

            /** Updates in un-committed transactions should not be visible **/
            beginTx@Database()(txHandle)
            update@Database({
                update = "INSERT INTO testTable(id, testString) VALUES (42, 'NewTestUser');"
                txHandle = txHandle
            })()
            query@Database("SELECT * FROM testTable;")(queryResponse)

            if (queryResponse.queryResponse.row[0].id != 1337){
                throw( TestFailed, "Updates executed within a transaction should not be visible outside that transaction" )
            }
            rollbackTx@Database(txHandle)()
            resetDatabase

            /** Committing a transaction makes updates visible outside the transaction **/
            beginTx@Database()(txHandle)
            update@Database({
                update = "INSERT INTO testTable(id, testString) VALUES (42, 'NewTestUser');"
                txHandle = txHandle
            })()
            commitTx@Database(txHandle)()
            query@Database("SELECT * FROM testTable;")(queryAfterCommit)
            if (#queryAfterCommit.row != 2){
                throw( TestFailed, "Committing a transaction did not result in the changes being stored" )
            }
            resetDatabase

            /** Rolling back a transaction closes the transaction **/
            beginTx@Database()(txHandle)
            update@Database({
                update = "INSERT INTO testTable(id, testString) VALUES (42, 'NewTestUser');"
                txHandle = txHandle
            })(o)
            rollbackTx@Database(s.txHandle)()
            scope (ShouldThrow){
                install(TransactionException => {x = true})
                query@Database({
                    query = "SELECT * FROM testTable;"
                    txHandle = s.txHandle
                })(queryResponse)
            }

            if (!is_defined(ShouldThrow.TransactionException)){
                throw( TestFailed, "Rolling back a transaction did not result in the transaction handle becomming invalid" )
            }
            resetDatabase

            /** Two different transactions get different handles **/
            beginTx@Database()(txHandles[0])
            beginTx@Database()(txHandles[1])
            if (txHandles[0] == txHandles[1]){
                throw( TestFailed, "Two different transactions were assigned the same handle" )
            }
            resetDatabase

            /** Providing hikariCP configs changes the behaviour of the Database Service **/
            newConnection << global.connection
            newConnection.connectionPoolConfig.readOnly = true
            connect@Database(newConnection)()
            scope (ShouldThrow){
                install(SQLException => {x = true})
                update@Database("INSERT INTO testTable(id, testString) VALUES (42, 'NewTestUser');")(o)
            }
            if (!is_defined(ShouldThrow.SQLException)){
                throw( TestFailed, "Providing a configuration options did not change the behaviour" )
            }
            resetDatabase

            /** Provoking a connection leak triggers an exception **/
            newConnection << global.connection
            newConnection.connectionPoolConfig << {
                maximumPoolSize = 1         // Ensure only one connetion is in the pool
                connectionTimeout = 1000    // Wait 1 second for a connection
            }

            connect@Database(newConnection)()
            beginTx@Database()(txHandle)    // Mark the only connection as 'busy'

            // Act
            scope (ShouldThrow){
                install (SQLException => {x = true})                
                beginTx@Database()(txHandle)    // Try to open a second connection
            }

            if (!is_defined(ShouldThrow.SQLException)){
                throw( TestFailed, "Opening more transactions than connections in the connection pool did not fail" )
            }
        }
    }
}