package hibiscus.cetide.app.log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseAuditSystem implements AuditSystem {
    private Connection connection;

    public DatabaseAuditSystem(String url, String user, String password) throws SQLException {
        connection = DriverManager.getConnection(url, user, password);
    }

    @Override
    public void audit(String logLine) {
        String sql = "INSERT INTO audit_logs (log_line) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, logLine);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}