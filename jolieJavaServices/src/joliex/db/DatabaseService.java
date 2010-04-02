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

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import jolie.runtime.CanUseJars;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;

/**
 * @author Fabrizio Montesi
 * 2008 - Marco Montesi: connection string fix for Microsoft SQL Server
 * 2009 - Claudio Guidi: added support for SQLite
 */
@CanUseJars({
	"derby.jar",			// Java DB - Embedded
	"derbyclient.jar",		// Java DB - Client
	"jdbc-mysql.jar",		// MySQL
	"jdbc-postgresql.jar",	// PostgreSQL
	"jdbc-sqlserver.jar",	// Microsoft SQLServer
	"jdbc-sqlite.jar"		// SQLite
})
public class DatabaseService extends JavaService
{
	private Connection connection = null;
	private String connectionString = null;
	private String username = null;
	private String password = null;
	private boolean mustCheckConnection = false;

	private final Object transactionMutex = new Object();

	@Override
	protected void finalize()
	{
		if ( connection != null ) {
			try {
				connection.close();
			} catch( SQLException e ) {}
		}
	}

	@RequestResponse
	public void connect( Value request )
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

		mustCheckConnection = request.getFirstChild( "checkConnection" ).intValue() > 0;

		String driver = request.getChildren( "driver" ).first().strValue();
		String host = request.getChildren( "host" ).first().strValue();
		String port = request.getChildren( "port" ).first().strValue();
		String databaseName = request.getChildren( "database" ).first().strValue();
		username = request.getChildren( "username" ).first().strValue();
		password = request.getChildren( "password" ).first().strValue();
		String separator = "/";
		try {
			if ( "postgresql".equals( driver ) ) {
				Class.forName( "org.postgresql.Driver" );
			} else if ( "mysql".equals( driver ) ) {
				Class.forName( "com.mysql.jdbc.Driver" );
			} else if ( "derby".equals( driver ) ) {
				Class.forName( "org.apache.derby.jdbc.ClientDriver" );
			} else if ( "sqlite".equals( driver ) ) {
				Class.forName( "org.sqlite.JDBC" );
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
	
	public Value update( String query )
		throws FaultException
	{
		checkConnection();
		Value resultValue = Value.create();
		try {
			synchronized( transactionMutex ) {
				Statement stm = connection.createStatement();
				resultValue.setValue( stm.executeUpdate( query ) );
				stm.close();
			}
		} catch( SQLException e ) {
			throw new FaultException( e );
		}
		return resultValue;
	}

	private static void resultSetToValueVector( ResultSet result, ValueVector vector )
		throws SQLException
	{
		Value rowValue, fieldValue;
		ResultSetMetaData metadata = result.getMetaData();
		int cols = metadata.getColumnCount();
		int i;
		int rowIndex = 0;
		while( result.next() ) {
			rowValue = vector.get( rowIndex );
			for( i = 1; i <= cols; i++ ) {
				fieldValue = rowValue.getFirstChild( metadata.getColumnLabel( i ) );
				switch( metadata.getColumnType( i ) ) {
				case java.sql.Types.INTEGER:
				case java.sql.Types.SMALLINT:
				case java.sql.Types.TINYINT:
					fieldValue.setValue( result.getInt( i ) );
					break;
				case java.sql.Types.BIGINT:
					// TODO: to be changed when getting support for Long in Jolie.
					fieldValue.setValue( result.getInt( i ) );
					break;
				case java.sql.Types.DOUBLE:
					fieldValue.setValue( result.getDouble( i ) );
					break;
				case java.sql.Types.FLOAT:
					fieldValue.setValue( result.getFloat( i ) );
					break;
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
					String s = result.getNString( i );
					if ( s == null ) {
						s = "";
					}
					fieldValue.setValue( s );
					break;
				case java.sql.Types.NUMERIC:
					BigDecimal dec = result.getBigDecimal( i );
					if ( dec == null ) {
						fieldValue.setValue( 0 );
					} else {
						if ( dec.scale() <= 0 ) {
							// May lose information.
							// Pay some attention to this when Long becomes supported by JOLIE.
							fieldValue.setValue( dec.intValue() );
						} else if ( dec.scale() > 0 ) {
							fieldValue.setValue( dec.doubleValue() );
						}
					}
					break;
				case java.sql.Types.VARCHAR:
				default:
					String str = result.getString( i );
					if ( str == null ) {
						str = "";
					}
					fieldValue.setValue( str );
					break;
				}
			}
			rowIndex++;
		}
	}

	public Value executeTransaction( Value request )
		throws FaultException
	{
		checkConnection();
		Value resultValue = Value.create();
		ValueVector resultVector = resultValue.getChildren( "result" );
		try {
			synchronized( transactionMutex ) {
				connection.setAutoCommit( false );
				Value currResultValue;
				for( Value statementValue : request.getChildren( "statement" ) ) {
					currResultValue = Value.create();
					Statement stm = connection.createStatement();
					if ( stm.execute( statementValue.strValue() ) == true ) {
						resultSetToValueVector( stm.getResultSet(), currResultValue.getChildren( "row" ) );
					}
					resultVector.add( currResultValue );
					stm.close();
				}
				connection.commit();
				connection.setAutoCommit( true );
			}
		} catch( SQLException e ) {
			throw new FaultException( e );
		}
		return resultValue;
	}
	
	public Value query( String query )
		throws FaultException
	{
		checkConnection();
		Value resultValue = Value.create();
		try {
			synchronized( transactionMutex ) {
				Statement stm = connection.createStatement();
				ResultSet result = stm.executeQuery( query );
				resultSetToValueVector( result, resultValue.getChildren( "row" ) );
				stm.close();
			}
		} catch( SQLException e ) {
			throw new FaultException( "SQLException", e );
		}
		
		return resultValue;
	}
}
