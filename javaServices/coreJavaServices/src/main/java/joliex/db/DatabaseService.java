/***************************************************************************
 *   Copyright (C) 2008-2014 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// Connection Pooling
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Optional;

import jolie.runtime.ByteArray;
import jolie.runtime.CanUseJars;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;
import joliex.db.impl.NamedStatementParser;

/**
 * @author Fabrizio Montesi 2008 - Marco Montesi: connection string fix for Microsoft SQL Server
 *         (2009) - Claudio Guidi: added support for SQLite (2013) - Matthias Dieter Wallnöfer:
 *         added support for HSQLDB (2013)
 */
@CanUseJars( {
	"derby.jar", // Java DB - Embedded
	"derbyclient.jar", // Java DB - Client
	"jdbc-mysql.jar", // MySQL
	"jdbc-postgresql.jar", // PostgreSQL
	"jdbc-sqlserver.jar", // Microsoft SQLServer
	"jdbc-sqlite.jar", // SQLite
	"jt400.jar", // AS400
	"hsqldb.jar", // HSQLDB
	"db2jcc.jar", // DB2
	"HikariCP.jar", // Connection Pool
	"slf4j-api.jar", // Logger API needed by CP
	"slf4j-nop.jar", // Logger implementation
} )
public class DatabaseService extends JavaService {
	private HikariDataSource connectionPool = null;
	private ConcurrentHashMap< Long, Connection > openTxs = null;
	private AtomicLong txHandles = null;

	private String connectionString = null;
	private String username = null;
	private String password = null;
	private String driver = null;
	private String driverClass = null;
	private static boolean toLowerCase = false;
	private static boolean toUpperCase = false;
	private boolean mustCheckConnection = false;
	private final static String TEMPLATE_FIELD = "_template";

	@RequestResponse
	public void close() {
		if( connectionPool != null ) {
			connectionString = null;
			username = null;
			password = null;
			driver = null;
			driverClass = null;
			_closeConnectionPool();
		}
	}

	@RequestResponse
	public void connect( Value request )
		throws FaultException {
		close();

		openTxs = new ConcurrentHashMap<>();
		txHandles = new AtomicLong();

		mustCheckConnection = request.getFirstChild( "checkConnection" ).intValue() > 0;

		toLowerCase = request.getFirstChild( "toLowerCase" ).isDefined()
			&& request.getFirstChild( "toLowerCase" ).boolValue();

		toUpperCase = request.getFirstChild( "toUpperCase" ).isDefined()
			&& request.getFirstChild( "toUpperCase" ).boolValue();

		driver = request.getChildren( "driver" ).first().strValue();
		if( request.getFirstChild( "driver" ).hasChildren( "class" ) ) {
			driverClass = request.getFirstChild( "driver" ).getFirstChild( "class" ).strValue();
		}
		String host = request.getChildren( "host" ).first().strValue();
		String port = request.getChildren( "port" ).first().strValue();
		String databaseName = request.getChildren( "database" ).first().strValue();
		username = request.getChildren( "username" ).first().strValue();
		password = request.getChildren( "password" ).first().strValue();
		String attributes = request.getFirstChild( "attributes" ).strValue();
		String separator = "/";
		boolean isEmbedded = false;
		Optional< String > encoding = Optional
			.ofNullable( request.hasChildren( "encoding" ) ? request.getFirstChild( "encoding" ).strValue() : null );
		try {
			if( driverClass == null ) {
				switch( driver ) {
				case "postgresql":
					driverClass = "org.postgresql.Driver";
					break;
				case "mysql":
					driverClass = "com.mysql.jdbc.Driver";
					break;
				case "derby":
					driverClass = "org.apache.derby.jdbc.ClientDriver";
					break;
				case "sqlite":
					driverClass = "org.sqlite.JDBC";
					isEmbedded = true;
					break;
				case "sqlserver":
					driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
					separator = ";";
					databaseName = "databaseName=" + databaseName;
					break;
				case "as400":
					driverClass = "com.ibm.as400.access.AS400JDBCDriver";
					break;
				case "derby_embedded":
					driverClass = "org.apache.derby.jdbc.EmbeddedDriver";
					isEmbedded = true;
					driver = "derby";
					break;
				case "hsqldb_embedded":
					isEmbedded = true;
					driver = "hsqldb";
				case "hsqldb_hsql":
				case "hsqldb_hsqls":
				case "hsqldb_http":
				case "hsqldb_https":
					driverClass = "org.hsqldb.jdbc.JDBCDriver";
					break;
				case "db2":
					driverClass = "com.ibm.db2.jcc.DB2Driver";
					break;
				default:
					throw new FaultException( "InvalidDriver", "Unknown type of driver: " + driver );
				}
			}
			Class.forName( driverClass );

			if( isEmbedded ) {
				connectionString = "jdbc:" + driver + ":" + databaseName;
				if( !attributes.isEmpty() ) {
					connectionString += ";" + attributes;
				}
				connectionPool = _createDataSource( request.getFirstChild( "connectionPoolConfig" ) );
				if( !"hsqldb".equals( driver ) ) { // driver == sqlite || driver == derby_embedded
					connectionPool.setUsername( null );
					connectionPool.setPassword( null );
				}
			} else {
				if( driver.startsWith( "hsqldb" ) ) {
					connectionString = "jdbc:" + driver + ":" + driver.substring( driver.indexOf( '_' ) + 1 ) + "//"
						+ host + (port.isEmpty() ? "" : ":" + port) + separator + databaseName;
				} else {
					connectionString = "jdbc:" + driver + "://" + host + (port.isEmpty() ? "" : ":" + port) + separator
						+ databaseName;
				}
				if( encoding.isPresent() ) {
					connectionString += "?characterEncoding=" + encoding.get();
				}
				connectionPool = _createDataSource( request.getFirstChild( "connectionPoolConfig" ) );
			}

			interpreter().cleaner().register( this, () -> {
				_closeConnectionPool();
			} );
		} catch( ClassNotFoundException e ) {
			throw new FaultException( "DriverClassNotFound", e );
		}
	}

	private void _checkConnection()
		throws FaultException {
		if( connectionPool == null ) {
			throw new FaultException( "ConnectionError" );
		}
		if( mustCheckConnection ) {
			if( connectionPool.isClosed() ) {
				connectionPool = new HikariDataSource( (HikariConfig) connectionPool.getHikariConfigMXBean() );
			}
		}
	}

	@RequestResponse
	public void checkConnection()
		throws FaultException {
		if( connectionPool == null || connectionPool.isClosed() ) {
			throw new FaultException( "ConnectionError" );
		}
	}

	@RequestResponse
	public Value update( Value request )
		throws FaultException {
		_checkConnection();
		Value resultValue = Value.create();

		if( request.isString() ) {
			try( Connection con = connectionPool.getConnection() ) {
				PreparedStatement stm = new NamedStatementParser( con, request.strValue(), request )
					.getPreparedStatement();
				resultValue.setValue( stm.executeUpdate() );
			} catch( SQLException e ) {
				throw createFaultException( e );
			}
		} else {
			long txHandle = request.getFirstChild( "txHandle" ).longValue();
			Connection tx = _getOpenTransaction( txHandle );

			try( PreparedStatement stm = new NamedStatementParser( tx, request.getFirstChild( "query" ).strValue(),
				request.getFirstChild( "query" ) )
					.getPreparedStatement(); ) {
				resultValue.setValue( stm.executeUpdate() );
				openTxs.put( txHandle, tx );
			} catch( SQLException e ) {
				throw createFaultException( e );
			}
		}
		return resultValue;
	}

	private Connection _getOpenTransaction( long txHandle ) throws FaultException {
		Connection tx = openTxs.remove( txHandle );
		if( tx == null ) {
			throw createTransactionException( "Transaction " + txHandle + " is unavailable or closed" );
		}
		return tx;
	}

	private static void setValue( Value fieldValue, ResultSet result, int columnType, int index )
		throws SQLException {
		ByteArray supportByteArray;
		switch( columnType ) {
		case java.sql.Types.INTEGER:
		case java.sql.Types.SMALLINT:
		case java.sql.Types.TINYINT:
			fieldValue.setValue( result.getInt( index ) );
			break;
		case java.sql.Types.BIGINT:
			fieldValue.setValue( result.getLong( index ) );
			break;
		case java.sql.Types.REAL:
		case java.sql.Types.DOUBLE:
			fieldValue.setValue( result.getDouble( index ) );
			break;
		case java.sql.Types.DECIMAL: {
			BigDecimal dec = result.getBigDecimal( index );
			if( dec == null ) {
				fieldValue.setValue( 0 );
			} else {
				if( dec.scale() <= 0 ) {
					// May lose information.
					// Pay some attention to this when Long becomes supported by JOLIE.
					fieldValue.setValue( dec.intValue() );
				} else if( dec.scale() > 0 ) {
					fieldValue.setValue( dec.doubleValue() );
				}
			}
		}
			break;
		case java.sql.Types.FLOAT:
			fieldValue.setValue( result.getFloat( index ) );
			break;
		case java.sql.Types.BLOB:
			supportByteArray = new ByteArray( result.getBytes( index ) );
			fieldValue.setValue( supportByteArray );
			break;
		case java.sql.Types.CLOB:
			Clob clob = result.getClob( index );
			fieldValue.setValue( clob.getSubString( 1, (int) clob.length() ) );
			break;
		case java.sql.Types.BINARY:
			supportByteArray = new ByteArray( result.getBytes( index ) );
			fieldValue.setValue( supportByteArray );
			break;
		case java.sql.Types.VARBINARY:
			supportByteArray = new ByteArray( result.getBytes( index ) );
			fieldValue.setValue( supportByteArray );
			break;
		case Types.LONGVARBINARY:
			supportByteArray = new ByteArray( result.getBytes( index ) );
			fieldValue.setValue( supportByteArray );
			break;
		case java.sql.Types.NVARCHAR:
		case java.sql.Types.NCHAR:
		case java.sql.Types.LONGNVARCHAR:
			String s = result.getNString( index );
			if( s == null ) {
				s = "";
			}
			fieldValue.setValue( s );
			break;
		case java.sql.Types.NUMERIC: {
			BigDecimal dec = result.getBigDecimal( index );

			if( dec == null ) {
				fieldValue.setValue( 0 );
			} else {
				if( dec.scale() <= 0 ) {
					// May lose information.
					// Pay some attention to this when Long becomes supported by JOLIE.
					fieldValue.setValue( dec.intValue() );
				} else if( dec.scale() > 0 ) {
					fieldValue.setValue( dec.doubleValue() );
				}
			}
		}
			break;
		case java.sql.Types.BIT:
		case java.sql.Types.BOOLEAN:
			fieldValue.setValue( result.getBoolean( index ) );
			break;
		case java.sql.Types.VARCHAR:
		default:
			String str = result.getString( index );
			if( str == null ) {
				str = "";
			}
			fieldValue.setValue( str );
			break;
		}
	}

	private static void resultSetToValueVector( ResultSet result, ValueVector vector )
		throws SQLException {
		Value rowValue, fieldValue;
		ResultSetMetaData metadata = result.getMetaData();
		int cols = metadata.getColumnCount();
		int i;
		int rowIndex = 0;
		if( toLowerCase ) {
			while( result.next() ) {
				rowValue = vector.get( rowIndex );
				for( i = 1; i <= cols; i++ ) {
					fieldValue = rowValue.getFirstChild( metadata.getColumnLabel( i ).toLowerCase() );
					setValue( fieldValue, result, metadata.getColumnType( i ), i );
				}
				rowIndex++;
			}
		} else if( toUpperCase ) {
			while( result.next() ) {
				rowValue = vector.get( rowIndex );
				for( i = 1; i <= cols; i++ ) {
					fieldValue = rowValue.getFirstChild( metadata.getColumnLabel( i ).toUpperCase() );
					setValue( fieldValue, result, metadata.getColumnType( i ), i );
				}
				rowIndex++;
			}
		} else {
			while( result.next() ) {
				rowValue = vector.get( rowIndex );
				for( i = 1; i <= cols; i++ ) {
					fieldValue = rowValue.getFirstChild( metadata.getColumnLabel( i ) );
					setValue( fieldValue, result, metadata.getColumnType( i ), i );
				}
				rowIndex++;
			}
		}
	}

	private static void _rowToValueWithTemplate(
		Value resultValue, ResultSet result,
		ResultSetMetaData metadata, Map< String, Integer > colIndexes,
		Value template )
		throws SQLException {
		Value templateNode;
		Value resultChild;
		int colIndex;
		for( Entry< String, ValueVector > child : template.children().entrySet() ) {
			templateNode = template.getFirstChild( child.getKey() );
			resultChild = resultValue.getFirstChild( child.getKey() );
			if( templateNode.isString() ) {
				colIndex = colIndexes.get( templateNode.strValue() );
				setValue( resultChild, result, metadata.getColumnType( colIndex ), colIndex );
			}

			_rowToValueWithTemplate( resultChild, result, metadata, colIndexes, templateNode );
		}
	}

	private static void resultSetToValueVectorWithTemplate( ResultSet result, ValueVector vector, Value template )
		throws SQLException {
		Value rowValue;
		ResultSetMetaData metadata = result.getMetaData();
		Map< String, Integer > colIndexes = new HashMap<>();
		int cols = metadata.getColumnCount();
		for( int i = 0; i < cols; i++ ) {
			colIndexes.put( metadata.getColumnName( i ), i );
		}

		int rowIndex = 0;
		while( result.next() ) {
			rowValue = vector.get( rowIndex );
			_rowToValueWithTemplate( rowValue, result, metadata, colIndexes, template );

			rowIndex++;
		}
	}

	@RequestResponse
	public Value executeTransaction( Value request )
		throws FaultException {
		_checkConnection();
		Value resultValue = Value.create();
		ValueVector resultVector = resultValue.getChildren( "result" );
		try( Connection connection = connectionPool.getConnection() ) {
			connection.setAutoCommit( false );
			Value currResultValue;
			int updateCount;

			for( Value statementValue : request.getChildren( "statement" ) ) {
				currResultValue = Value.create();
				try( PreparedStatement stm = new NamedStatementParser( connection, statementValue.strValue(),
					statementValue )
						.getPreparedStatement() ) {
					updateCount = -1;
					if( stm.execute() == true ) {
						updateCount = stm.getUpdateCount();
						if( updateCount == -1 ) {
							if( statementValue.hasChildren( TEMPLATE_FIELD ) ) {
								resultSetToValueVectorWithTemplate( stm.getResultSet(),
									currResultValue.getChildren( "row" ),
									statementValue.getFirstChild( TEMPLATE_FIELD ) );
							} else {
								resultSetToValueVector( stm.getResultSet(), currResultValue.getChildren( "row" ) );
							}
							stm.getResultSet().close();
						}
					}
					currResultValue.setValue( updateCount );
					resultVector.add( currResultValue );
				} catch( SQLException e ) {
					try {
						connection.rollback();
					} catch( SQLException e1 ) {
						close();
					}
					throw createFaultException( e );
				}
			}
			try {
				connection.commit();
			} catch( SQLException e ) {
				connection.rollback();
				throw createFaultException( e );
			} finally {
				connection.setAutoCommit( true );
			}
		} catch( SQLException e ) {
			throw createFaultException( e );
		}
		return resultValue;
	}

	static FaultException createFaultException( SQLException e ) {
		Value v = Value.create();
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		e.printStackTrace( new PrintStream( bs ) );
		v.getNewChild( "stackTrace" ).setValue( bs.toString() );
		v.getNewChild( "errorCode" ).setValue( e.getErrorCode() );
		v.getNewChild( "SQLState" ).setValue( e.getSQLState() );
		v.getNewChild( "message" ).setValue( e.getMessage() );
		return new FaultException( "SQLException", v );
	}

	@RequestResponse
	public Value query( Value request )
		throws FaultException {
		_checkConnection();
		Value resultValue;

		if( request.isString() ) {
			try( Connection con = connectionPool.getConnection();
				PreparedStatement stm = new NamedStatementParser( con, request.strValue(), request )
					.getPreparedStatement() ) {
				resultValue = _executeQuery( stm, request );
			} catch( SQLException e ) {
				throw createFaultException( e );
			}
		} else {
			long txHandle = request.getFirstChild( "txHandle" ).longValue();
			Connection tx = _getOpenTransaction( txHandle );
			try( PreparedStatement stm = new NamedStatementParser( tx, request.getFirstChild( "query" ).strValue(),
				request.getFirstChild( "query" ) )
					.getPreparedStatement(); ) {
				resultValue = _executeQuery( stm, request.getFirstChild( "query" ) );
				openTxs.put( txHandle, tx );
			} catch( SQLException e ) {
				throw createFaultException( e );
			}
		}
		return resultValue;
	}

	private FaultException createTransactionException( String message ) {
		Value v = Value.create();
		v.getNewChild( "message" ).setValue( message );
		return new FaultException( "TransactionException", v );
	}

	private Value _executeQuery( PreparedStatement stm, Value request ) throws SQLException {
		Value resultValue = Value.create();
		ResultSet result = stm.executeQuery();
		if( request.hasChildren( TEMPLATE_FIELD ) ) {
			resultSetToValueVectorWithTemplate( result, resultValue.getChildren( "row" ),
				request.getFirstChild( TEMPLATE_FIELD ) );
		} else {
			resultSetToValueVector( result, resultValue.getChildren( "row" ) );
		}
		result.close();
		return resultValue;
	}

	@RequestResponse
	public Value beginTx() throws FaultException {
		_checkConnection();
		Value response = Value.create();
		Connection con;
		try {
			con = connectionPool.getConnection();
			long txHandle = txHandles.getAndIncrement();
			con.setAutoCommit( false );

			openTxs.put( txHandle, con );

			response.setValue( txHandle );
			return response;
		} catch( SQLException e ) {
			throw createFaultException( e );
		}
	}

	@RequestResponse
	public void commitTx( Value request ) throws FaultException {
		_checkConnection();

		long txHandle = request.longValue();
		Connection tx = openTxs.remove( txHandle );
		try {
			tx.commit();
		} catch( SQLException e ) {
			createFaultException( e );
		} finally {
			_closeTransaction( tx );
		}
	}

	@RequestResponse
	public void rollbackTx( Value request ) throws FaultException {
		_checkConnection();

		long txHandle = request.longValue();
		Connection tx = openTxs.remove( txHandle );
		_closeTransaction( tx );
	}

	private void _closeTransaction( Connection con ) throws FaultException {
		try {
			con.setAutoCommit( true );
			con.rollback();
			con.close();
		} catch( SQLException e ) {
			try {
				con.close();
			} catch( SQLException e1 ) {
				throw createFaultException( e );
			}
		}
	}

	private void _closeConnectionPool() {
		for( long handle : openTxs.keySet() ) {
			try {
				Connection con = openTxs.remove( handle );
				con.rollback();
				con.close();
			} catch( SQLException e ) {
			}
		}
		openTxs.clear();
		connectionPool.close();
	}

	private HikariDataSource _createDataSource( Value providedConfig ) {
		HikariConfig config = new HikariConfig();
		config.setUsername( username );
		config.setPassword( password );
		config.setDriverClassName( driverClass );
		config.setJdbcUrl( connectionString );

		_setUserprovidedConfig( config, providedConfig );

		return new HikariDataSource( config );
	}

	private void _setUserprovidedConfig( HikariConfig config, Value providedConfig ) {
		if( providedConfig.hasChildren() ) {
			if( providedConfig.hasChildren( "connectionTimeout" ) ) {
				config.setConnectionTimeout( providedConfig.getFirstChild( "connectionTimeout" ).longValue() );
			}
			if( providedConfig.hasChildren( "idleTimeout" ) ) {
				config.setIdleTimeout( providedConfig.getFirstChild( "idleTimeout" ).longValue() );
			}
			if( providedConfig.hasChildren( "maxLifetime" ) ) {
				config.setMaxLifetime( providedConfig.getFirstChild( "maxLifetime" ).longValue() );
			}
			if( providedConfig.hasChildren( "connectionTestQuery" ) ) {
				config.setConnectionTestQuery( providedConfig.getFirstChild( "connectionTestQuery" ).strValue() );
			}
			if( providedConfig.hasChildren( "minimumIdle" ) ) {
				config.setMinimumIdle( providedConfig.getFirstChild( "minimumIdle" ).intValue() );
			}
			if( providedConfig.hasChildren( "maximumPoolSize" ) ) {
				config.setMaximumPoolSize( providedConfig.getFirstChild( "maximumPoolSize" ).intValue() );
			}
			if( providedConfig.hasChildren( "poolName" ) ) {
				config.setPoolName( providedConfig.getFirstChild( "poolName" ).strValue() );
			}
			if( providedConfig.hasChildren( "initializationFailTimeout" ) ) {
				config.setInitializationFailTimeout(
					providedConfig.getFirstChild( "initializationFailTimeout" ).longValue() );
			}
			if( providedConfig.hasChildren( "isolateInternalQueries" ) ) {
				config
					.setIsolateInternalQueries( providedConfig.getFirstChild( "isolateInternalQueries" ).boolValue() );
			}
			if( providedConfig.hasChildren( "readOnly" ) ) {
				config
					.setReadOnly( providedConfig.getFirstChild( "readOnly" ).boolValue() );
			}
			if( providedConfig.hasChildren( "catalog" ) ) {
				config
					.setCatalog( providedConfig.getFirstChild( "catalog" ).strValue() );
			}
			if( providedConfig.hasChildren( "connectionInitSql" ) ) {
				config
					.setConnectionInitSql( providedConfig.getFirstChild( "connectionInitSql" ).strValue() );
			}
			if( providedConfig.hasChildren( "transactionIsolation" ) ) {
				config
					.setTransactionIsolation( providedConfig.getFirstChild( "transactionIsolation" ).strValue() );
			}
			if( providedConfig.hasChildren( "validationTimeout" ) ) {
				config
					.setValidationTimeout( providedConfig.getFirstChild( "validationTimeout" ).longValue() );
			}
		}
	}
}
