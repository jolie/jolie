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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import jolie.runtime.ByteArray;
import jolie.runtime.CanUseJars;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;
import joliex.db.impl.NamedStatementParser;
import joliex.db.impl.ConnectionPool;

/**
 * @author Fabrizio Montesi 2008 - Marco Montesi: defaultConnection string fix
 *         for
 *         Microsoft SQL Server
 *         (2009) - Claudio Guidi: added support for SQLite (2013) - Matthias
 *         Dieter Wallnöfer:
 *         added support for HSQLDB (2013)
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
		"db2jcc.jar" // DB2
})
public class DatabaseService extends JavaService {
	private Connection defaultConnection = null;
	private ConnectionPool connectionPool = null;
	private String connectionString = null;
	private String username = null;
	private String password = null;
	private String driver = null;
	private String driverClass = null;
	private static boolean toLowerCase = false;
	private static boolean toUpperCase = false;
	private boolean mustCheckConnection = false;
	private final Object transactionMutex = new Object();
	private final Object connectionPoolMutex = new Object();
	private final static String TEMPLATE_FIELD = "_template";

	@RequestResponse
	public void close() {
		synchronized (connectionPoolMutex) {
			if (defaultConnection != null) {
				try {
					connectionString = null;
					username = null;
					password = null;
					connectionPool.close(defaultConnection);
				} catch (SQLException e) {
				}
			}
		}
	}

	@RequestResponse
	public void connect(Value request)
			throws FaultException {
		close();

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
		Optional<String> encoding = Optional
				.ofNullable(request.hasChildren("encoding") ? request.getFirstChild("encoding").strValue() : null);

		try {
			if (driverClass == null) {
				if ("postgresql".equals(driver)) {
					Class.forName("org.postgresql.Driver");
				} else if ("mysql".equals(driver)) {
					Class.forName("com.mysql.jdbc.Driver");
				} else if ("derby".equals(driver)) {
					Class.forName("org.apache.derby.jdbc.ClientDriver");
				} else if ("sqlite".equals(driver)) {
					Class.forName("org.sqlite.JDBC");
					isEmbedded = true;
				} else if ("sqlserver".equals(driver)) {
					Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
					separator = ";";
					databaseName = "databaseName=" + databaseName;
				} else if ("as400".equals(driver)) {
					Class.forName("com.ibm.as400.access.AS400JDBCDriver");
				} else if ("derby_embedded".equals(driver)) {
					Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
					isEmbedded = true;
					driver = "derby";
				} else if ("hsqldb_hsql".equals(driver)
						|| "hsqldb_hsqls".equals(driver)
						|| "hsqldb_http".equals(driver)
						|| "hsqldb_https".equals(driver)) {
					Class.forName("org.hsqldb.jdbcDriver");
				} else if ("hsqldb_embedded".equals(driver)) {
					Class.forName("org.hsqldb.jdbcDriver");
					isEmbedded = true;
					driver = "hsqldb";
				} else if ("db2".equals(driver)) {
					Class.forName("com.ibm.db2.jcc.DB2Driver");
				} else {
					throw new FaultException("InvalidDriver", "Unknown type of driver: " + driver);
				}
			} else {
				Class.forName(driverClass);
			}

			if (isEmbedded) {
				connectionString = "jdbc:" + driver + ":" + databaseName;
				if (!attributes.isEmpty()) {
					connectionString += ";" + attributes;
				}
				if ("hsqldb".equals(driver)) {
					connectionPool = new ConnectionPool(connectionString, username, password);
					defaultConnection = connectionPool.getFreeConnection();
				} else {
					connectionPool = new ConnectionPool(connectionString, username, password);
					defaultConnection = connectionPool.getFreeConnection();
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
				connectionPool = new ConnectionPool(connectionString, username, password);
				defaultConnection = connectionPool.getFreeConnection();
			}

			if (defaultConnection == null) {
				throw new FaultException("ConnectionError");
			} else {
				final ConnectionPool conPool = connectionPool;
				interpreter().cleaner().register(this, () -> {
					try {
						conPool.closeAll();
					} catch (SQLException e) {
					}
				});
			}
		} catch (ClassNotFoundException e) {
			throw new FaultException("DriverClassNotFound", e);
		} catch (SQLException e) {
			throw createFaultException(e);
		}
	}

	private void _checkConnection()
			throws FaultException {
		if (defaultConnection == null) {
			throw new FaultException("ConnectionError");
		}
		if (mustCheckConnection) {
			try {
				if ("postgresql".equals(driver)) {
					/*
					 * The JDBC4 driver for postgresql does not implemented isValid(). We fallback
					 * to isClosed().
					 */

					if (defaultConnection.isClosed()) {
						defaultConnection = connectionPool.getFreeConnection();
					}
				} else {
					if (!defaultConnection.isValid(0)) {
						defaultConnection = connectionPool.getFreeConnection();
					}
				}
			} catch (SQLException e) {
				throw createFaultException(e);
			}
		}
	}

	@RequestResponse
	public void checkConnection()
			throws FaultException {
		try {
			if ("postgresql".equals(driver)) {
				/*
				 * The JDBC4 driver for postgresql does not implemented isValid(). We fallback
				 * to isClosed().
				 */

				if (defaultConnection == null || defaultConnection.isClosed()) {
					throw new FaultException("ConnectionError");
				}
			} else {

				if (defaultConnection == null || !defaultConnection.isValid(0)) {
					throw new FaultException("ConnectionError");
				}
			}
		} catch (SQLException e) {
			throw createFaultException(e);
		}
	}

	@RequestResponse
	public Value update(Value request)
			throws FaultException {
		_checkConnection();
		Value resultValue = Value.create();
		try (PreparedStatement stm = new NamedStatementParser(defaultConnection, request.strValue(), request)
				.getPreparedStatement()) {
			synchronized (transactionMutex) {
				resultValue.setValue(stm.executeUpdate());
			}
		} catch (SQLException e) {
			throw createFaultException(e);
		}
		return resultValue;
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
		synchronized (transactionMutex) {
			try {
				defaultConnection.setAutoCommit(false);
			} catch (SQLException e) {
				throw createFaultException(e);
			}

			Value currResultValue;
			int updateCount;

			for (Value statementValue : request.getChildren("statement")) {
				currResultValue = Value.create();
				try (PreparedStatement stm = new NamedStatementParser(defaultConnection, statementValue.strValue(),
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
						defaultConnection.rollback();
					} catch (SQLException e1) {
						defaultConnection = null;
					}
					throw createFaultException(e);
				}
			}

			try {
				defaultConnection.commit();
			} catch (SQLException e) {
				throw createFaultException(e);
			} finally {
				try {
					defaultConnection.setAutoCommit(true);
				} catch (SQLException e) {
					throw createFaultException(e);
				}
			}
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
		Value resultValue = Value.create();

		synchronized (transactionMutex) {
			try (PreparedStatement stm = new NamedStatementParser(defaultConnection, request.strValue(), request)
					.getPreparedStatement()) {
				ResultSet result = stm.executeQuery();
				if (request.hasChildren(TEMPLATE_FIELD)) {
					resultSetToValueVectorWithTemplate(result, resultValue.getChildren("row"),
							request.getFirstChild(TEMPLATE_FIELD));
				} else {
					resultSetToValueVector(result, resultValue.getChildren("row"));
				}
				result.close();
			} catch (SQLException e) {
				throw createFaultException(e);
			}
		}

		return resultValue;
	}
}
