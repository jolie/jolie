/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

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
	private String connectionString = null;
	private String username = null;
	private String password = null;
	private boolean mustCheckConnection = false;

	@Override
	protected void finalize()
	{
		if ( connection != null ) {
			try {
				connection.close();
			} catch( SQLException e ) {}
		}
	}

	public CommMessage connect( CommMessage message )
		throws FaultException
	{
		if ( connection != null ) {
			try {
				connectionString = null;
				username = null;
				password = null;
				connection.close();
			} catch( SQLException e ) {}
		}

		mustCheckConnection = message.value().getFirstChild( "checkConnection" ).intValue() > 0;

		String driver = message.value().getChildren( "driver" ).first().strValue();
		String host = message.value().getChildren( "host" ).first().strValue();
		String port = message.value().getChildren( "port" ).first().strValue();
		String databaseName = message.value().getChildren( "database" ).first().strValue();
		username = message.value().getChildren( "username" ).first().strValue();
		password = message.value().getChildren( "password" ).first().strValue();
		String separator = "/";
		try {
			if ( "postgresql".equals( driver ) ) {
				Class.forName( "org.postgresql.Driver" );
			} else if ( "mysql".equals( driver ) ) {
				Class.forName( "com.mysql.jdbc.Driver" );
			} else if ( "derby".equals( driver ) ) {
				Class.forName( "org.apache.derby.jdbc.ClientDriver" );
			} else if ( "sqlserver".equals( driver ) ) {
				//Class.forName( "com.microsoft.sqlserver.jdbc.SQLServerDriver" );
				separator = ";";
				databaseName = "databaseName=" + databaseName;
			} else if ( "as400".equals( driver ) ) {
				Class.forName( "com.ibm.as400.access.AS400JDBCDriver" );
			} else {
				throw new FaultException( "InvalidDriver", "Uknown driver: " + driver );
			}

			connectionString = "jdbc:"+ driver + "://" + host + ( port.equals( "" ) ? "" : ":" + port ) + separator + databaseName;
			connection = DriverManager.getConnection(
						connectionString,
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
		return CommMessage.createResponse( message, Value.create() );
	}

	private void checkConnection()
		throws FaultException
	{
		if ( connection == null ) {
			throw new FaultException( "ConnectionError" );
		}
		if ( mustCheckConnection ) {
			try {
				if ( !connection.isValid( 0 ) ) {
					connection = DriverManager.getConnection(
							connectionString,
							username,
							password
						);
				}
			} catch( SQLException e ) {
				throw new FaultException( e );
			}
		}
	}
	
	public CommMessage update( CommMessage request )
		throws FaultException
	{
		checkConnection();
		Value resultValue = Value.create();
		String query = request.value().strValue();
		try {
			Statement stm = connection.createStatement();
			resultValue.setValue( stm.executeUpdate( query ) );
		} catch( SQLException e ) {
			throw new FaultException( e );
		}
		return CommMessage.createResponse( request, resultValue );
	}
	
	public CommMessage query( CommMessage request )
		throws FaultException
	{
		checkConnection();
		Value resultValue = Value.create();
		Value rowValue, fieldValue;
		String query = request.value().strValue();
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
		
		return CommMessage.createResponse( request, resultValue );
	}
}
