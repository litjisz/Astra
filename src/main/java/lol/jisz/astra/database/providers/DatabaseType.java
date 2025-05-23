package lol.jisz.astra.database.providers;

public enum DatabaseType {
    MONGODB("MongoDB"),
    MYSQL("MySQL"),
    SQLITE("SQLite"),
    POSTGRESQL("PostgreSQL"),
    MARIADB("MariaDB"),
    NONE("None");

    private final String name;

    DatabaseType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DatabaseType fromString(String name) {
        for (DatabaseType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown database type: " + name);
    }
}
