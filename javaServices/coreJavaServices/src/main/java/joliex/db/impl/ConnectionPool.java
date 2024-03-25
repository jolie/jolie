package joliex.db.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import jolie.util.Pair;

/**
 * A class implementing a connection pool for JDBC connections.
 *
 * @author Mathias Jensen
 */

public class ConnectionPool {
    private ConcurrentHashMap<String, Connection> freeConnections = null;
    private ConcurrentHashMap<String, Connection> usedConnections = null;
    private final Object connectionMapLock = new Object();

    // Required parameters
    private String connectionString;
    private String username;
    private String password;
    private int initialPoolSize;
    private int maxPoolSize;

    public ConnectionPool(String connectionString, String username, String password) throws SQLException {
        this(connectionString, username, password, 6, 8);
    }

    public ConnectionPool(String connectionString, String username, String password, int initialPoolSize,
            int maxPoolSize)
            throws SQLException {
        freeConnections = new ConcurrentHashMap<String, Connection>();
        usedConnections = new ConcurrentHashMap<String, Connection>();

        this.connectionString = connectionString;
        this.username = username;
        this.password = password;
        this.initialPoolSize = initialPoolSize;
        this.maxPoolSize = maxPoolSize;

        for (int i = 0; i < this.initialPoolSize; i++) {
            String handle = String.format("%f%s", Thread.currentThread().getId(),
                    java.util.UUID.randomUUID().toString());

            freeConnections.put(handle, DriverManager.getConnection(
                    connectionString,
                    username,
                    password));
        }
    }

    /**
     * Returns an unused connection from the connection pool. If the pool has no
     * unused connections, a new connection will be created and added to the pool.
     * 
     * @return Connection
     * @throws SQLException
     */
    public Connection getFreeConnection() throws SQLException {
        String handle;
        Connection con;
        Iterator<Entry<String, Connection>> it = freeConnections.entrySet().iterator();

        if (!it.hasNext()) { // All connections are in use, we need to allocate a new
            synchronized (connectionMapLock) {
                // TODO: Figure out if this is the best way to handle a fully used pool
                Pair<String, Connection> newConnection = _createNewFreeConnection();
                handle = newConnection.key();
                con = newConnection.value();
            }
        } else {
            Entry<String, Connection> entry = it.next();
            handle = entry.getKey();
            con = entry.getValue();
            synchronized (connectionMapLock) {
                freeConnections.remove(entry.getKey());
                usedConnections.put(handle, con);
            }
        }
        return con;
    }

    /**
     * Returns a specific connection from the connection pool, or null if a
     * connection with the specified handle does not exist.
     * 
     * @param connectionHandle
     * @return Connection
     */
    public Connection getConnection(String connectionHandle) {
        synchronized (connectionMapLock) {
            return _getConnection(connectionHandle);
        }
    }

    /**
     * Releases the connection associated with connectionHandle back to the
     * connection pool
     * 
     * @param connectionHandle
     * @throws SQLException
     */
    public void releaseConnection(String connectionHandle) throws SQLException {
        synchronized (connectionMapLock) {
            Connection con = usedConnections.get(connectionHandle);
            if (con == null) {
                return;
            }
            _resetConnection(con);

            usedConnections.remove(connectionHandle);
            freeConnections.put(connectionHandle, con);
        }
    }

    /**
     * Closes the connection associated with connectionHandle, then removes it from
     * the connectionPool and creates a new connection in its stead.
     * 
     * @param connectionHandle
     * @throws SQLException
     */
    public void close(String connectionHandle) throws SQLException {
        synchronized (connectionMapLock) {
            Connection con = _getConnection(connectionHandle);
            if (con == null) {
                return;

            }
            _removeConnection(connectionHandle);
            _createNewFreeConnection();
            con.close();
        }
    }

    /**
     * Closes all connections managed by this connection pool.
     * 
     * @throws SQLException
     */
    public void closeAll() throws SQLException {
        synchronized (connectionMapLock) {
            for (Connection con : freeConnections.values()) {
                con.close();
            }
            for (Connection con : usedConnections.values()) {
                con.rollback();
                con.close();
            }
        }
    }

    private void _resetConnection(Connection con) throws SQLException {
        con.rollback();
        con.setAutoCommit(true);
    }

    private void _removeConnection(String connectionHandle) {
        freeConnections.remove(connectionHandle);
        usedConnections.remove(connectionHandle);
    }

    private Connection _getConnection(String connectionHandle) {
        return usedConnections.get(connectionHandle) != null ? usedConnections.get(connectionHandle)
                : freeConnections.get(connectionHandle);
    }

    private Pair<String, Connection> _createNewFreeConnection() throws SQLException {
        String handle = String.format("%f%s", Thread.currentThread().getId(),
                java.util.UUID.randomUUID().toString());
        Connection con = DriverManager.getConnection(connectionString, username, password);
        freeConnections.put(handle, con);
        return new Pair<String, Connection>(handle, con);
    }
}
