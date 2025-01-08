package hibiscus.cetide.app.core.model;


public class DatabaseConfig {
    private String dbType;
    private String host;
    private String port;
    private String database;
    private String username;
    private String password;

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return String.format("jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC", 
                    host, port, database);
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%s:%s", host, port, database);
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%s;databaseName=%s", host, port, database);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }
} 