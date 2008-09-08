package joliex.db;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import jolie.net.CommMessage;
import jolie.runtime.CanUseJars;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

/**
 * @author Fabrizio Montesi
 * 2008 - Marco Montesi: connection string fix for Microsoft SQL Server
 */
@CanUseJars({
	"derby.jar",			// Java DB - Embedded
	"derbyclient.jar",		// Java DB - Client
	"jdbc-mysql.jar",		// MySQL
	"jdbc-postgresql.jar",	// PostgreSQL
	"jdbc-sqlserver.jar"	// Microsoft SQLServer
})
public class DatabaseService extends JavaService
{
	private Connection connection = null;
	
	public CommMessage connect( CommMessage message )
		throws FaultException
	{
		String driver = message.value().getChildren( "driver" ).first().strValue();
		String host = message.value().getChildren( "host" ).first().strValue();
		String port = message.value().getChildren( "port" ).first().strValue();
		String databaseName = message.value().getChildren( "database" ).first().strValue();
		String username = message.value().getChildren( "username" ).first().strValue();
		String password = message.value().getChildren( "password" ).first().strValue();
		String separator = "/";
		try {
			if ( "postgresql".equals( driver ) ) {
				Class.forName( "org.postgresql.Driver" );
			} else if ( "mysql".equals( driver ) ) {
				Class.forName( "com.mysql.jdbc.Driver" );
			} else if ( "derby".equals( driver ) ) {
				Class.forName( "org.apache.derby.jdbc.ClientDriver" );
			} else if ( "sqlserver".equals( driver ) ) {
				Class.forName( "com.microsoft.sqlserver.jdbc.SQLServerDriver" );
				separator = ";";
				databaseName = "databaseName=" + databaseName;
			} else if ( "as400".equals( driver ) ) {
				Class.forName( "com.ibm.as400.access.AS400JDBCDriver" );
			} else {
				throw new FaultException( "InvalidDriver", "Uknown driver: " + driver );
			}

			connection = DriverManager.getConnection(
						"jdbc:"+ driver + "://" + host + ( port.equals( "" ) ? "" : ":" + port ) + separator + databaseName,
						username,
						password
					);
			if ( connection == null ) {
				throw new FaultException( "ConnectionError" );
			}
		} catch( ClassNotFoundException e ) {
			throw new FaultException( "InvalidDriver", e );
		} catch( SQLException e ) {
			throw new FaultException( "ConnectionError", e );
		}
		return null;
	}
	
	public CommMessage update( CommMessage message )
		throws FaultException
	{
		Value resultValue = Value.create();
		String query = message.value().strValue();
		try {
			Statement stm = connection.createStatement();
			resultValue.setValue( stm.executeUpdate( query ) );
		} catch( SQLException e ) {
			throw new FaultException( e );
		}
		return new CommMessage( "update", "/", resultValue );
	}
	
	public CommMessage query( CommMessage message )
		throws FaultException
	{
		Value resultValue = Value.create();
		Value rowValue, fieldValue;
		String query = message.value().strValue();
		try {
			Statement stm = connection.createStatement();
			ResultSet result = stm.executeQuery( query );
			ResultSetMetaData metadata = result.getMetaData();
			int cols = metadata.getColumnCount();
			int i;
			int rowIndex = 0;
			while( result.next() ) {
				rowValue = resultValue.getChildren( "row" ).get( rowIndex );
				for( i = 1; i <= cols; i++ ) {
					fieldValue = rowValue.getChildren( metadata.getColumnLabel( i ) ).first();
					switch( metadata.getColumnType( i ) ) {
					case java.sql.Types.BLOB:
						//fieldValue.setStrValue( result.getBlob( i ).toString() );
						break;
					case java.sql.Types.CLOB:
						Clob clob = result.getClob( i );
						fieldValue.setValue( clob.getSubString( 0L, (int)clob.length() ) );
						break;
					case java.sql.Types.NVARCHAR:
					case java.sql.Types.NCHAR:
					case java.sql.Types.LONGNVARCHAR:
						fieldValue.setValue( result.getNString( i ) );
						break;
					case java.sql.Types.VARCHAR:
					default:
						fieldValue.setValue( result.getString( i ) );
						break;
					}
				}
				rowIndex++;
			}
		} catch( SQLException e ) {
			throw new FaultException( "SQLException", e );
		}
		
		return new CommMessage( "query", "/", resultValue );
	}
}
