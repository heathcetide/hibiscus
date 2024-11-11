package hibiscus.cetide.app.common.model;

import java.sql.Connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionDetails {
    private Connection connection;
    private final String driverClassName;
    private final String dbType;
    private final String url;
    private final String username;
    private final String password;

    public ConnectionDetails(Connection connection, String driverClassName, String dbType, String url, String username, String password) {
        this.driverClassName = driverClassName;
        this.dbType = dbType;
        this.connection = connection;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public ConnectionDetails(String driverClassName, String dbType, String url, String username, String password) {
        this.driverClassName = driverClassName;
        this.dbType = dbType;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName(driverClassName);
                connection = DriverManager.getConnection(url, username, password);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Driver class not found: " + driverClassName, e);
            }
        }
        return connection;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getDbType() {
        return dbType;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "ConnectionDetails{" +
                "connection=" + connection +
                ", driverClassName='" + driverClassName + '\'' +
                ", dbType='" + dbType + '\'' +
                ", url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
