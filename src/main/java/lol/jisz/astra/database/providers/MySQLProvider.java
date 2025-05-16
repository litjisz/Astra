package lol.jisz.astra.database.providers;

import lol.jisz.astra.Astra;
import lol.jisz.astra.database.DatabaseProvider;
import lol.jisz.astra.database.DatabaseType;
import lol.jisz.astra.utils.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

/**
 * MySQL implementation of the DatabaseProvider interface.
 */
public class MySQLProvider implements DatabaseProvider {
    private final Logger logger;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String options;
    
    private Connection connection;

    /**
     * Constructs a new MySQL database provider with default connection options.
     *
     * @param plugin   The Astra plugin instance providing the logger
     * @param host     The hostname or IP address of the MySQL server
     * @param port     The port number on which the MySQL server is running
     * @param database The name of the database to connect to
     * @param username The username for authentication
     * @param password The password for authentication
     */
    public MySQLProvider(Astra plugin, String host, int port, String database, String username, String password) {
        this(plugin, host, port, database, username, password, "useSSL=false&autoReconnect=true");
    }

    /**
     * Constructs a new MySQL database provider with custom connection options.
     *
     * @param plugin   The Astra plugin instance providing the logger
     * @param host     The hostname or IP address of the MySQL server
     * @param port     The port number on which the MySQL server is running
     * @param database The name of the database to connect to
     * @param username The username for authentication
     * @param password The password for authentication
     * @param options  Additional JDBC connection options as a URL query string
     */
    public MySQLProvider(Astra plugin, String host, int port, String database, String username, String password, String options) {
        this.logger = plugin.logger();
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.options = options;
    }

    /**
     * Initializes the database connection.
     * Loads the MySQL JDBC driver and establishes a connection to the database.
     */
    @Override
    public void initialize() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            if (options != null && !options.isEmpty()) {
                url += "?" + options;
            }
            connection = DriverManager.getConnection(url, username, password);
            logger.info("Connected to MySQL database: " + database);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Failed to connect to MySQL", e);
        }
    }

    /**
     * Gets the current database connection, creating a new one if necessary.
     *
     * @return The active database connection
     * @throws Exception If a connection cannot be established
     */
    @Override
    public Connection getConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            if (options != null && !options.isEmpty()) {
                url += "?" + options;
            }
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }

    /**
     * Closes the database connection if it is open.
     */
    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("MySQL connection closed");
            }
        } catch (SQLException e) {
            logger.error("Error closing MySQL connection", e);
        }
    }

    /**
     * Checks if the database connection is valid and active.
     *
     * @return true if the connection is valid, false otherwise
     */
    @Override
    public boolean isValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            logger.error("Error checking MySQL connection validity", e);
            return false;
        }
    }

    /**
     * Returns the type of database this provider implements.
     *
     * @return The database type (MYSQL)
     */
    @Override
    public DatabaseType getType() {
        return DatabaseType.MYSQL;
    }

    /**
     * Creates a new table in the database if it doesn't already exist.
     *
     * @param name The name of the table to create
     * @return true if the table was created successfully, false otherwise
     */
    @Override
    public boolean createCollection(String name) {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS " + name + " (id INT AUTO_INCREMENT PRIMARY KEY)";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Failed to create table: " + name, e);
            return false;
        }
    }

    /**
     * Inserts a new record into the specified table.
     * Automatically creates any missing columns based on the data provided.
     *
     * @param collection The name of the table to insert into
     * @param data       A map containing column names and values to insert
     * @return true if the insertion was successful, false otherwise
     */
    @Override
    public boolean insert(String collection, Map<String, Object> data) {
        if (data.isEmpty()) {
            return false;
        }

        try {
            ensureColumnsExist(collection, data);
            
            StringJoiner columns = new StringJoiner(", ");
            StringJoiner placeholders = new StringJoiner(", ");
            
            for (String key : data.keySet()) {
                columns.add(key);
                placeholders.add("?");
            }
            
            String sql = "INSERT INTO " + collection + " (" + columns + ") VALUES (" + placeholders + ")";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int i = 1;
                for (Object value : data.values()) {
                    pstmt.setObject(i++, value);
                }
                
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.error("Failed to insert data into table: " + collection, e);
            return false;
        }
    }

    /**
     * Asynchronously inserts a new record into the specified table.
     *
     * @param collection The name of the table to insert into
     * @param data       A map containing column names and values to insert
     * @return A CompletableFuture that will resolve to true if the insertion was successful, false otherwise
     */
    @Override
    public CompletableFuture<Boolean> insertAsync(String collection, Map<String, Object> data) {
        return CompletableFuture.supplyAsync(() -> insert(collection, data));
    }

    /**
     * Updates records in the specified table that match the given filter criteria.
     * Automatically creates any missing columns based on the update data provided.
     *
     * @param collection The name of the table to update
     * @param filter     A map containing column names and values to filter records by
     * @param updates    A map containing column names and values to update
     * @return The number of records updated
     */
    @Override
    public int update(String collection, Map<String, Object> filter, Map<String, Object> updates) {
        if (updates.isEmpty()) {
            return 0;
        }

        try {
            ensureColumnsExist(collection, updates);
            
            StringJoiner setClause = new StringJoiner(", ");
            for (String key : updates.keySet()) {
                setClause.add(key + " = ?");
            }
            
            String whereClause = buildWhereClause(filter);
            
            String sql = "UPDATE " + collection + " SET " + setClause + whereClause;
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int i = 1;
                
                for (Object value : updates.values()) {
                    pstmt.setObject(i++, value);
                }
                
                if (!filter.isEmpty()) {
                    for (Object value : filter.values()) {
                        pstmt.setObject(i++, value);
                    }
                }
                
                return pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to update data in table: " + collection, e);
            return 0;
        }
    }

    /**
     * Asynchronously updates records in the specified table that match the given filter criteria.
     *
     * @param collection The name of the table to update
     * @param filter     A map containing column names and values to filter records by
     * @param updates    A map containing column names and values to update
     * @return A CompletableFuture that will resolve to the number of records updated
     */
    @Override
    public CompletableFuture<Integer> updateAsync(String collection, Map<String, Object> filter, Map<String, Object> updates) {
        return CompletableFuture.supplyAsync(() -> update(collection, filter, updates));
    }

    /**
     * Finds records in the specified table that match the given filter criteria.
     *
     * @param collection The name of the table to query
     * @param filter     A map containing column names and values to filter records by
     * @return A list of maps, where each map represents a record with column names and values
     */
    @Override
    public List<Map<String, Object>> find(String collection, Map<String, Object> filter) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            String whereClause = buildWhereClause(filter);
            String sql = "SELECT * FROM " + collection + whereClause;
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int i = 1;
                if (!filter.isEmpty()) {
                    for (Object value : filter.values()) {
                        pstmt.setObject(i++, value);
                    }
                }
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int j = 1; j <= columnCount; j++) {
                            String columnName = metaData.getColumnName(j);
                            Object value = rs.getObject(j);
                            row.put(columnName, value);
                        }
                        results.add(row);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find data in table: " + collection, e);
        }
        
        return results;
    }

    /**
     * Asynchronously finds records in the specified table that match the given filter criteria.
     *
     * @param collection The name of the table to query
     * @param filter     A map containing column names and values to filter records by
     * @return A CompletableFuture that will resolve to a list of maps, where each map represents a record
     */
    @Override
    public CompletableFuture<List<Map<String, Object>>> findAsync(String collection, Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> find(collection, filter));
    }

    /**
     * Deletes records from the specified table that match the given filter criteria.
     *
     * @param collection The name of the table to delete from
     * @param filter     A map containing column names and values to filter records by
     * @return The number of records deleted
     */
    @Override
    public int delete(String collection, Map<String, Object> filter) {
        try {
            String whereClause = buildWhereClause(filter);
            String sql = "DELETE FROM " + collection + whereClause;
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int i = 1;
                if (!filter.isEmpty()) {
                    for (Object value : filter.values()) {
                        pstmt.setObject(i++, value);
                    }
                }
                
                return pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to delete data from table: " + collection, e);
            return 0;
        }
    }

    /**
     * Asynchronously deletes records from the specified table that match the given filter criteria.
     *
     * @param collection The name of the table to delete from
     * @param filter     A map containing column names and values to filter records by
     * @return A CompletableFuture that will resolve to the number of records deleted
     */
    @Override
    public CompletableFuture<Integer> deleteAsync(String collection, Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> delete(collection, filter));
    }

    /**
     * Builds a SQL WHERE clause from a map of filter criteria.
     *
     * @param filter A map containing column names and values to filter by
     * @return A formatted SQL WHERE clause, or an empty string if no filter is provided
     */
    private String buildWhereClause(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return "";
        }
        
        StringJoiner conditions = new StringJoiner(" AND ", " WHERE ", "");
        for (String key : filter.keySet()) {
            conditions.add(key + " = ?");
        }
        
        return conditions.toString();
    }

    /**
     * Ensures that all columns required by the data exist in the table, creating them if necessary.
     *
     * @param table The name of the table to check
     * @param data  A map containing column names and values to ensure exist
     * @throws SQLException If there is an error accessing the database
     */
    private void ensureColumnsExist(String table, Map<String, Object> data) throws SQLException {
        if (data.isEmpty()) {
            return;
        }
        
        Map<String, String> existingColumns = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM " + table)) {
            
            while (rs.next()) {
                existingColumns.put(rs.getString("Field"), rs.getString("Type"));
            }
        }
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String columnName = entry.getKey();
            if (!existingColumns.containsKey(columnName) && !columnName.equals("id")) {
                String columnType = getSQLType(entry.getValue());
                String alterSql = "ALTER TABLE " + table + " ADD COLUMN " + columnName + " " + columnType;
                
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(alterSql);
                }
            }
        }
    }

    /**
     * Determines the appropriate SQL data type for a Java object.
     *
     * @param value The Java object to determine the SQL type for
     * @return The SQL data type as a string
     */
    private String getSQLType(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return "BIGINT";
        } else if (value instanceof Float || value instanceof Double) {
            return "DOUBLE";
        } else if (value instanceof Boolean) {
            return "BOOLEAN";
        } else {
            return "TEXT";
        }
    }
}