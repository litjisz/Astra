package lol.jisz.astra.database.providers;

import lol.jisz.astra.Astra;
import lol.jisz.astra.database.DatabaseProvider;
import lol.jisz.astra.database.DatabaseType;
import lol.jisz.astra.utils.Logger;

import java.io.File;
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
 * SQLite implementation of the DatabaseProvider interface.
 */
public class SQLiteProvider implements DatabaseProvider {
    private final Logger logger;
    private final String databaseName;
    private final File databaseFile;
    
    private Connection connection;

    /**
     * Constructs a new SQLiteProvider instance.
     *
     * @param plugin The Astra plugin instance that provides access to the logger and data folder
     * @param databaseName The name of the database to be created or connected to (without extension)
     */
    public SQLiteProvider(Astra plugin, String databaseName) {
        this.logger = plugin.logger();
        this.databaseName = databaseName;
        
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        this.databaseFile = new File(plugin.getDataFolder(), databaseName + ".db");
    }

    /**
     * Initializes the SQLite database connection.
     * Loads the JDBC driver and establishes a connection to the database file.
     * Enables foreign key constraints.
     */
    @Override
    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            
            logger.info("Connected to SQLite database: " + databaseName);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Failed to connect to SQLite", e);
        }
    }

    /**
     * Gets the current database connection or creates a new one if needed.
     *
     * @return The active database connection
     * @throws SQLException If a database access error occurs
     */
    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
        }
        return connection;
    }

    /**
     * Closes the database connection if it is open.
     * Logs the closure or any errors that occur during the process.
     */
    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("SQLite connection closed");
            }
        } catch (SQLException e) {
            logger.error("Error closing SQLite connection", e);
        }
    }

    /**
     * Checks if the database connection is valid by executing a simple query.
     *
     * @return true if the connection is valid and operational, false otherwise
     */
    @Override
    public boolean isValid() {
        try {
            if (connection != null && !connection.isClosed()) {
                try (Statement stmt = connection.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
                        return rs.next();
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            logger.error("Error checking database connection validity", e);
            return false;
        }
    }

    /**
     * Returns the type of database being used.
     *
     * @return The database type (SQLITE)
     */
    @Override
    public DatabaseType getType() {
        return DatabaseType.SQLITE;
    }

    /**
     * Creates a new collection (table) in the database if it doesn't already exist.
     *
     * @param name The name of the collection/table to create
     * @return true if the collection was created successfully, false otherwise
     */
    @Override
    public boolean createCollection(String name) {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS " + name + " (id INTEGER PRIMARY KEY AUTOINCREMENT)";
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
     * Inserts a new record into the specified collection.
     * Automatically creates any missing columns based on the data provided.
     *
     * @param collection The name of the collection/table to insert into
     * @param data A map containing column names and values to insert
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
     * Asynchronously inserts a new record into the specified collection.
     *
     * @param collection The name of the collection/table to insert into
     * @param data A map containing column names and values to insert
     * @return A CompletableFuture that will resolve to true if the insertion was successful, false otherwise
     */
    @Override
    public CompletableFuture<Boolean> insertAsync(String collection, Map<String, Object> data) {
        return CompletableFuture.supplyAsync(() -> insert(collection, data));
    }

    /**
     * Updates records in the specified collection that match the filter criteria.
     * Automatically creates any missing columns based on the update data provided.
     *
     * @param collection The name of the collection/table to update
     * @param filter A map containing column names and values to filter records by
     * @param updates A map containing column names and values to update
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
     * Asynchronously updates records in the specified collection that match the filter criteria.
     *
     * @param collection The name of the collection/table to update
     * @param filter A map containing column names and values to filter records by
     * @param updates A map containing column names and values to update
     * @return A CompletableFuture that will resolve to the number of records updated
     */
    @Override
    public CompletableFuture<Integer> updateAsync(String collection, Map<String, Object> filter, Map<String, Object> updates) {
        return CompletableFuture.supplyAsync(() -> update(collection, filter, updates));
    }

    /**
     * Finds records in the specified collection that match the filter criteria.
     *
     * @param collection The name of the collection/table to search in
     * @param filter A map containing column names and values to filter records by
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
     * Asynchronously finds records in the specified collection that match the filter criteria.
     *
     * @param collection The name of the collection/table to search in
     * @param filter A map containing column names and values to filter records by
     * @return A CompletableFuture that will resolve to a list of maps, where each map represents a record
     */
    @Override
    public CompletableFuture<List<Map<String, Object>>> findAsync(String collection, Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> find(collection, filter));
    }

    /**
     * Deletes records from the specified collection that match the filter criteria.
     *
     * @param collection The name of the collection/table to delete from
     * @param filter A map containing column names and values to filter records by
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
     * Asynchronously deletes records from the specified collection that match the filter criteria.
     *
     * @param collection The name of the collection/table to delete from
     * @param filter A map containing column names and values to filter records by
     * @return A CompletableFuture that will resolve to the number of records deleted
     */
    @Override
    public CompletableFuture<Integer> deleteAsync(String collection, Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> delete(collection, filter));
    }

    /**
     * Builds a SQL WHERE clause from a map of filter criteria.
     *
     * @param filter A map containing column names and values to filter records by
     * @return A formatted WHERE clause string, or an empty string if no filter is provided
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
     * Ensures that all columns required by the data exist in the table.
     * Creates any missing columns with appropriate data types.
     *
     * @param table The name of the table to check/modify
     * @param data A map containing column names and values to ensure exist
     * @throws SQLException If a database access error occurs
     */
    private void ensureColumnsExist(String table, Map<String, Object> data) throws SQLException {
        if (data.isEmpty()) {
            return;
        }
        
        Map<String, String> existingColumns = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            
            while (rs.next()) {
                existingColumns.put(rs.getString("name"), rs.getString("type"));
            }
        }
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String columnName = entry.getKey();
            if (!existingColumns.containsKey(columnName) && !columnName.equals("id")) {
                String columnType = getSQLiteType(entry.getValue());
                String alterSql = "ALTER TABLE " + table + " ADD COLUMN " + columnName + " " + columnType;
                
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(alterSql);
                }
            }
        }
    }

    /**
     * Determines the appropriate SQLite data type for a Java object.
     *
     * @param value The Java object to determine the type for
     * @return The corresponding SQLite data type as a string
     */
    private String getSQLiteType(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return "INTEGER";
        } else if (value instanceof Float || value instanceof Double) {
            return "REAL";
        } else if (value instanceof Boolean) {
            return "INTEGER";
        } else {
            return "TEXT";
        }
    }
}