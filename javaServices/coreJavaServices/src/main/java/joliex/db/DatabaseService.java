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

// Connection Pooling
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Optional;
import java.util.UUID;

import jolie.runtime.ByteArray;
import jolie.runtime.CanUseJars;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;
import joliex.db.impl.NamedStatementParser;

/**
 * @author Fabrizio Montesi 2008 - Marco Montesi: defaultConnection string fix
 *         for Microsoft SQL
 *         Server (2009) - Claudio Guidi: added support for SQLite (2013) -
 *         Matthias Dieter
 *         Wallnöfer: added support for HSQLDB (2013)
 */
@CanUseJars({
		"derby.jar", // Java DB - Embedded
		"derbyclient.jar", // Java DB - Client
		"jdbc-mysql.jar", // MySQL
		"jdbc-postgresql.jar", // PostgreSQL
		"jdbc-sqlserver.jar", // Microsoft SQLServer
		"jdbc-sqlite.jar", // SQLite
		"jt400.jar", // AS400
		"hsqldb.jar", // HSQLDB
		"db2jcc.jar", // DB2
		"HikariCP.jar",
		"HikariCP-java7.jar",
		"slf4j-simple.jar",
		"slf4j-api.jar"
})
public class DatabaseService extends JavaService {
	private HikariDataSource connectionPoolDataSource = null;
	private ConcurrentHashMap<String, Object> transactionMutexes = null;
	private ConcurrentHashMap<String, Connection> openTransactions = null;

	private String connectionString = null;
	private String username = null;
	private String password = null;
	private String driver = null;
	private String driverClass = null;
	private static boolean toLowerCase = false;
	private static boolean toUpperCase = false;
	private boolean mustCheckConnection = false;
	private boolean transactionsEnabled = false;
	private final static String TEMPLATE_FIELD = "_template";

	@RequestResponse
	public void close() {
		if (connectionPoolDataSource != null) {
			connectionString = null;
			username = null;
			password = null;
			connectionPoolDataSource.close();
			connectionPoolDataSource = null;
			transactionMutexes.clear();

			openTransactions.forEach((key, con) -> {
				try {
					con.close();
				} catch (SQLException e) {
				}
			});
		}
	}

	@RequestResponse
	public void connect(Value request)
			throws FaultException {
		close();

		transactionMutexes = new ConcurrentHashMap<>();
		openTransactions = new ConcurrentHashMap<>();

		mustCheckConnection = request.getFirstChild("checkConnection").intValue() > 0;

		toLowerCase = request.getFirstChild("toLowerCase").isDefined()
				&& request.getFirstChild("toLowerCase").boolValue();

		toUpperCase = request.getFirstChild("toUpperCase").isDefined()
				&& request.getFirstChild("toUpperCase").boolValue();

		driver = request.getChildren("driver").first().strValue();
		if (request.getFirstChild("driver").hasChildren("class")) {
			driverClass = request.getFirstChild("driver").getFirstChild("class").strValue();
		}
		String host = request.getChildren("host").first().strValue();
		String port = request.getChildren("port").first().strValue();
		String databaseName = request.getChildren("database").first().strValue();
		username = request.getChildren("username").first().strValue();
		password = request.getChildren("password").first().strValue();
		String attributes = request.getFirstChild("attributes").strValue();
		String separator = "/";
		boolean isEmbedded = false;
		transactionsEnabled = false;
		Optional<String> encoding = Optional
				.ofNullable(request.hasChildren("encoding") ? request.getFirstChild("encoding").strValue() : null);
		try {
			switch (driver) {
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
					throw new FaultException("InvalidDriver", "Unknown type of driver: " + driver);
			}
			Class.forName(driverClass);

			if (isEmbedded) {
				connectionString = "jdbc:" + driver + ":" + databaseName;
				if (!attributes.isEmpty()) {
					connectionString += ";" + attributes;
				}
				connectionPoolDataSource = _createDataSource();
				if (!"hsqldb".equals(driver)) {
					connectionPoolDataSource.setUsername(null);
					connectionPoolDataSource.setPassword(null);
				}
			} else {
				if (driver.startsWith("hsqldb")) {
					connectionString = "jdbc:" + driver + ":" + driver.substring(driver.indexOf('_') + 1) + "//"
							+ host + (port.isEmpty() ? "" : ":" + port) + separator + databaseName;
				} else {
					connectionString = "jdbc:" + driver + "://" + host + (port.isEmpty() ? "" : ":" + port) + separator
							+ databaseName;
				}
				if (encoding.isPresent()) {
					connectionString += "?characterEncoding=" + encoding.get();
				}
				connectionPoolDataSource = _createDataSource();
			}

			if (connectionPoolDataSource == null) {
				throw new FaultException("ConnectionError");
			} else {
				interpreter().cleaner().register(this, () -> {
					connectionPoolDataSource.close();
				});
			}
		} catch (ClassNotFoundException e) {
			throw new FaultException("DriverClassNotFound", e);
		}
	}

	private void _checkConnection()
			throws FaultException {
		if (connectionPoolDataSource == null) {
			throw new FaultException("ConnectionError");
		}
		if (mustCheckConnection) {
			if (connectionPoolDataSource.isClosed()) {
				connectionPoolDataSource = _createDataSource();
			}
		}
	}

	@RequestResponse
	public void checkConnection()
			throws FaultException {
		if (connectionPoolDataSource == null || connectionPoolDataSource.isClosed()) {
			throw new FaultException("ConnectionError");
		}
	}

	@RequestResponse
	public Value update(Value request)
			throws FaultException {
		_checkConnection();
		Value resultValue = Value.create();

		if (request.hasChildren("transactionHandle")) {
			Connection con;
			String transactionHandle = request.getFirstChild("transactionHandle").strValue();

			if (!transactionMutexes.containsKey(transactionHandle)) {
				throw createTransactionException(
						"Transaction with handle '" + transactionHandle + "' does not exist or is no longer open.");
			}

			synchronized (transactionMutexes.get(transactionHandle)) {
				// ÆRØ:
				// When two threads both call 'get' simultaniously, they can both access
				// the lock. if the first one to execute then commits, the object is removed
				// from the transactionMutexes map, but since the reference to the object is
				// still present at the second thread,
				// the object is not deleted. The second thread can then enter the protected
				// block, and do work on a transaction that was already committed. Hence, this
				// check, which ensures that if the scenario above occurs, it is detected, and
				// no work is done.
				if (!transactionMutexes.containsKey(transactionHandle)) {
					throw createTransactionException(
							"Transaction with handle '" + transactionHandle + "' does not exist or is no longer open.");
				}

				con = openTransactions.get(transactionHandle);
				try (PreparedStatement stm = new NamedStatementParser(con, request.strValue(), request)
						.getPreparedStatement();) {
					resultValue.setValue(stm.executeUpdate());
				} catch (SQLException e) {
					throw createFaultException(e);
				}
			}
		} else {
			try (Connection con = connectionPoolDataSource.getConnection()) {
				PreparedStatement stm = new NamedStatementParser(con, request.strValue(), request)
						.getPreparedStatement();
				resultValue.setValue(stm.executeUpdate());
			} catch (SQLException e) {
				throw createFaultException(e);
			}
		}
		return resultValue;
	}

	private void _tryEnableTransactions() throws FaultException {
		if (!transactionsEnabled) {
			try {
				switch (driver) {
					case "hsqldb":
					case "hsqldb_embedded":
					case "hsqldb_hsql":
					case "hsqldb_hsqls":
					case "hsqldb_http":
					case "hsqldb_https":
						Connection con = connectionPoolDataSource.getConnection();
						PreparedStatement stm = con.prepareStatement("SET DATABASE TRANSACTION CONTROL MVCC;");
						stm.execute();
						con.close();
						stm.close();
						break;
					case "sqlite":
						throw createTransactionException("SQLite does not support manipulating multiple connections.");
				}
				transactionsEnabled = true;
			} catch (SQLException e) {
				throw createFaultException(e);
			}
		}
	}

	private static void setValue(Value fieldValue, ResultSet result, int columnType, int index)
			throws SQLException {
		ByteArray supportByteArray;
		switch (columnType) {
			case java.sql.Types.INTEGER:
			case java.sql.Types.SMALLINT:
			case java.sql.Types.TINYINT:
				fieldValue.setValue(result.getInt(index));
				break;
			case java.sql.Types.BIGINT:
				fieldValue.setValue(result.getLong(index));
				break;
			case java.sql.Types.REAL:
			case java.sql.Types.DOUBLE:
				fieldValue.setValue(result.getDouble(index));
				break;
			case java.sql.Types.DECIMAL: {
				BigDecimal dec = result.getBigDecimal(index);
				if (dec == null) {
					fieldValue.setValue(0);
				} else {
					if (dec.scale() <= 0) {
						// May lose information.
						// Pay some attention to this when Long becomes supported by JOLIE.
						fieldValue.setValue(dec.intValue());
					} else if (dec.scale() > 0) {
						fieldValue.setValue(dec.doubleValue());
					}
				}
			}
				break;
			case java.sql.Types.FLOAT:
				fieldValue.setValue(result.getFloat(index));
				break;
			case java.sql.Types.BLOB:
				supportByteArray = new ByteArray(result.getBytes(index));
				fieldValue.setValue(supportByteArray);
				break;
			case java.sql.Types.CLOB:
				Clob clob = result.getClob(index);
				fieldValue.setValue(clob.getSubString(1, (int) clob.length()));
				break;
			case java.sql.Types.BINARY:
				supportByteArray = new ByteArray(result.getBytes(index));
				fieldValue.setValue(supportByteArray);
				break;
			case java.sql.Types.VARBINARY:
				supportByteArray = new ByteArray(result.getBytes(index));
				fieldValue.setValue(supportByteArray);
				break;
			case Types.LONGVARBINARY:
				supportByteArray = new ByteArray(result.getBytes(index));
				fieldValue.setValue(supportByteArray);
				break;
			case java.sql.Types.NVARCHAR:
			case java.sql.Types.NCHAR:
			case java.sql.Types.LONGNVARCHAR:
				String s = result.getNString(index);
				if (s == null) {
					s = "";
				}
				fieldValue.setValue(s);
				break;
			case java.sql.Types.NUMERIC: {
				BigDecimal dec = result.getBigDecimal(index);

				if (dec == null) {
					fieldValue.setValue(0);
				} else {
					if (dec.scale() <= 0) {
						// May lose information.
						// Pay some attention to this when Long becomes supported by JOLIE.
						fieldValue.setValue(dec.intValue());
					} else if (dec.scale() > 0) {
						fieldValue.setValue(dec.doubleValue());
					}
				}
			}
				break;
			case java.sql.Types.BIT:
			case java.sql.Types.BOOLEAN:
				fieldValue.setValue(result.getBoolean(index));
				break;
			case java.sql.Types.VARCHAR:
			default:
				String str = result.getString(index);
				if (str == null) {
					str = "";
				}
				fieldValue.setValue(str);
				break;
		}
	}

	private static void resultSetToValueVector(ResultSet result, ValueVector vector)
			throws SQLException {
		Value rowValue, fieldValue;
		ResultSetMetaData metadata = result.getMetaData();
		int cols = metadata.getColumnCount();
		int i;
		int rowIndex = 0;
		if (toLowerCase) {
			while (result.next()) {
				rowValue = vector.get(rowIndex);
				for (i = 1; i <= cols; i++) {
					fieldValue = rowValue.getFirstChild(metadata.getColumnLabel(i).toLowerCase());
					setValue(fieldValue, result, metadata.getColumnType(i), i);
				}
				rowIndex++;
			}
		} else if (toUpperCase) {
			while (result.next()) {
				rowValue = vector.get(rowIndex);
				for (i = 1; i <= cols; i++) {
					fieldValue = rowValue.getFirstChild(metadata.getColumnLabel(i).toUpperCase());
					setValue(fieldValue, result, metadata.getColumnType(i), i);
				}
				rowIndex++;
			}
		} else {
			while (result.next()) {
				rowValue = vector.get(rowIndex);
				for (i = 1; i <= cols; i++) {
					fieldValue = rowValue.getFirstChild(metadata.getColumnLabel(i));
					setValue(fieldValue, result, metadata.getColumnType(i), i);
				}
				rowIndex++;
			}
		}
	}

	private static void _rowToValueWithTemplate(
			Value resultValue, ResultSet result,
			ResultSetMetaData metadata, Map<String, Integer> colIndexes,
			Value template)
			throws SQLException {
		Value templateNode;
		Value resultChild;
		int colIndex;
		for (Entry<String, ValueVector> child : template.children().entrySet()) {
			templateNode = template.getFirstChild(child.getKey());
			resultChild = resultValue.getFirstChild(child.getKey());
			if (templateNode.isString()) {
				colIndex = colIndexes.get(templateNode.strValue());
				setValue(resultChild, result, metadata.getColumnType(colIndex), colIndex);
			}

			_rowToValueWithTemplate(resultChild, result, metadata, colIndexes, templateNode);
		}
	}

	private static void resultSetToValueVectorWithTemplate(ResultSet result, ValueVector vector, Value template)
			throws SQLException {
		Value rowValue;
		ResultSetMetaData metadata = result.getMetaData();
		Map<String, Integer> colIndexes = new HashMap<>();
		int cols = metadata.getColumnCount();
		for (int i = 0; i < cols; i++) {
			colIndexes.put(metadata.getColumnName(i), i);
		}

		int rowIndex = 0;
		while (result.next()) {
			rowValue = vector.get(rowIndex);
			_rowToValueWithTemplate(rowValue, result, metadata, colIndexes, template);

			rowIndex++;
		}
	}

	@RequestResponse
	public Value executeTransaction(Value request)
			throws FaultException {
		_checkConnection();
		Value resultValue = Value.create();
		ValueVector resultVector = resultValue.getChildren("result");
		try (Connection con = connectionPoolDataSource.getConnection()) {
			con.setAutoCommit(false);
			Value currResultValue;
			int updateCount;

			for (Value statementValue : request.getChildren("statement")) {
				currResultValue = Value.create();
				try (PreparedStatement stm = new NamedStatementParser(con, statementValue.strValue(),
						statementValue)
						.getPreparedStatement()) {
					updateCount = -1;
					if (stm.execute() == true) {
						updateCount = stm.getUpdateCount();
						if (updateCount == -1) {
							if (statementValue.hasChildren(TEMPLATE_FIELD)) {
								resultSetToValueVectorWithTemplate(stm.getResultSet(),
										currResultValue.getChildren("row"),
										statementValue.getFirstChild(TEMPLATE_FIELD));
							} else {
								resultSetToValueVector(stm.getResultSet(), currResultValue.getChildren("row"));
							}
							stm.getResultSet().close();
						}
					}
					currResultValue.setValue(updateCount);
					resultVector.add(currResultValue);
				} catch (SQLException e) {
					try {
						con.rollback();
					} catch (SQLException e1) {
						// Something has gone totally wrong. Close the connectionpool
						connectionPoolDataSource.close();
					}
					throw createFaultException(e);
				}
			}
			try {
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw createFaultException(e);
			} finally {
				con.setAutoCommit(true);
			}
		} catch (SQLException e) {
			throw createFaultException(e);
		}
		return resultValue;
	}

	static FaultException createFaultException(SQLException e) {
		Value v = Value.create();
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(bs));
		v.getNewChild("stackTrace").setValue(bs.toString());
		v.getNewChild("errorCode").setValue(e.getErrorCode());
		v.getNewChild("SQLState").setValue(e.getSQLState());
		v.getNewChild("message").setValue(e.getMessage());
		return new FaultException("SQLException", v);
	}

	@RequestResponse
	public Value query(Value request)
			throws FaultException {
		_checkConnection();
		Value resultValue;

		if (request.hasChildren("transactionHandle")) {
			String transactionHandle = request.getFirstChild("transactionHandle").strValue();

			if (!transactionMutexes.containsKey(transactionHandle)) {
				throw createTransactionException(
						"Transaction with handle '" + transactionHandle + "' does not exist or is no longer open.");
			}

			synchronized (transactionMutexes.get(transactionHandle)) {
				// Ensure that no other thread has committed the transaction while this was
				// waiting to access
				if (!transactionMutexes.containsKey(transactionHandle)) {
					throw createTransactionException(
							"Transaction with handle '" + transactionHandle + "' does not exist or is no longer open.");
				}

				try (Connection con = openTransactions.get(transactionHandle);
						PreparedStatement stm = new NamedStatementParser(con, request.strValue(), request)
								.getPreparedStatement();) {
					resultValue = _executeQuery(stm, request);
				} catch (SQLException e) {
					throw createFaultException(e);
				}
			}
		} else {
			try (Connection con = connectionPoolDataSource.getConnection();
					PreparedStatement stm = new NamedStatementParser(con, request.strValue(), request)
							.getPreparedStatement()) {
				resultValue = _executeQuery(stm, request);
			} catch (SQLException e) {
				throw createFaultException(e);
			}
		}

		return resultValue;
	}

	private FaultException createTransactionException(String message) {
		Exception e = new Exception("Stack trace");
		Value v = Value.create();
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(bs));
		v.getNewChild("stackTrace").setValue(bs.toString());
		v.getNewChild("message")
				.setValue(message);
		return new FaultException("TransactionException", v);
	}

	private Value _executeQuery(PreparedStatement stm, Value request) throws SQLException {
		Value resultValue = Value.create();
		ResultSet result = stm.executeQuery();
		if (request.hasChildren(TEMPLATE_FIELD)) {
			resultSetToValueVectorWithTemplate(result, resultValue.getChildren("row"),
					request.getFirstChild(TEMPLATE_FIELD));
		} else {
			resultSetToValueVector(result, resultValue.getChildren("row"));
		}
		result.close();
		return resultValue;
	}

	@RequestResponse
	public Value initializeTransaction() throws FaultException {
		_checkConnection();
		_tryEnableTransactions();
		Value response = Value.create();
		Connection con;
		try {
			// Create a new connection, and map it to a generated UUID.
			con = connectionPoolDataSource.getConnection();
			String uuid = Thread.currentThread().getId() + UUID.randomUUID().toString();
			Object transactionLock = new Object();
			con.setAutoCommit(false);

			// Store the open transactions in a map. uuid is used as a handle.
			transactionMutexes.put(uuid, transactionLock);
			openTransactions.put(uuid, con);

			response.setValue(uuid);
			return response;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new FaultException("SQLException", "Error while starting transaction.");
		}
	}

	@RequestResponse
	public void commitTransaction(Value request) throws FaultException {
		_checkConnection();
		String transactionHandle = request.strValue();

		if (!transactionMutexes.containsKey(transactionHandle)) {
			throw createTransactionException(
					"Transaction with handle '" + transactionHandle + "' does not exist or is no longer open.");
		}

		synchronized (transactionMutexes.get(transactionHandle)) {
			// Ensure that no other thread has committed the transaction while this was
			// waiting to access
			if (!transactionMutexes.containsKey(transactionHandle)) {
				throw createTransactionException(
						"Transaction with handle '" + transactionHandle + "' does not exist or is no longer open.");
			}

			Connection con = openTransactions.get(transactionHandle);
			try {
				con.commit();
				con.setAutoCommit(false); // ÆRØ: Don't know if this is needed, or if HikariCP resets it on close
				openTransactions.remove(transactionHandle);
				transactionMutexes.remove(transactionHandle);
			} catch (SQLException e) {
				_closeTransaction(con, transactionHandle);
			}
		}

	}

	@RequestResponse
	public void abortTransaction(Value request) throws FaultException {
		_checkConnection();
		String transactionHandle = request.strValue();

		if (!transactionMutexes.containsKey(transactionHandle)) {
			throw createTransactionException(
					"Transaction with handle '" + transactionHandle + "' does not exist or is no longer open.");
		}

		synchronized (transactionMutexes.get(transactionHandle)) {
			// Ensure that no other thread has committed the transaction while this was
			// waiting to access
			if (!transactionMutexes.containsKey(transactionHandle)) {
				throw createTransactionException(
						"Transaction with handle '" + transactionHandle + "' does not exist or is no longer open.");
			}

			Connection con = openTransactions.get(transactionHandle);
			_closeTransaction(con, transactionHandle);
		}
	}

	private void _closeTransaction(Connection con, String transactionHandle) throws FaultException {
		SQLException exception = null;
		try {
			con.rollback();
			con.setAutoCommit(false);
			con.close();
		} catch (SQLException e) {
			exception = e;
			try {
				con.close();
			} catch (SQLException e1) {
			}
		} finally {
			transactionMutexes.remove(transactionHandle);
			openTransactions.remove(transactionHandle);
		}
		if (exception != null) {
			throw createFaultException(exception);
		}
	}

	private HikariDataSource _createDataSource() {
		HikariConfig config = new HikariConfig();

		config.setUsername(username);
		config.setPassword(password);
		config.setMaximumPoolSize(6); // TODO: ÆRØ Figure this out
		config.setJdbcUrl(connectionString);
		config.setDriverClassName(driverClass);

		// Disabeling leak detection menas that no error will be logged when a
		// conenction has been out of the pool for a long time.
		// This is what we want, we want to be able to remove a connection for multiple
		// seconds for inter-service transactions.
		config.setLeakDetectionThreshold(0);

		return new HikariDataSource(config);

	}
}
