package hibiscus.cetide.app.core.service;

import hibiscus.cetide.app.core.model.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.sql.*;
import java.util.*;

@Service
public class HibiscusDatabaseAdminService {
    
    @Value("${spring.config.location:}")
    private String configLocation;

    private static Logger log = LoggerFactory.getLogger(HibiscusDatabaseAdminService.class);

    private DatabaseConfig loadDatabaseConfig() {
        try {
            // 尝试从多个位置加载配置文件
            String[] configPaths = {
                    "temp/application.yml",
                "application.yaml",
                "config/application.yml",
                "config/application.yaml",
                "src/main/resources/application.yml",
                "src/main/resources/application.yaml"
            };

            Map<String, Object> config = null;
            
            // 如果指定了配置文件位置，先尝试从指定位置加载
            if (configLocation != null && !configLocation.isEmpty()) {
                config = loadYamlConfig(configLocation.replace("classpath:", ""));
            }
            
            // 如果没有找到配置，尝试其他位置
            if (config == null) {
                for (String path : configPaths) {
                    config = loadYamlConfig(path);
                    if (config != null) break;
                }
            }
            
            if (config == null) {
                log.warn("No configuration file found");
                return null;
            }

            // 处理配置文件中的数据库配置
            Map<String, Object> spring = (Map<String, Object>) config.get("spring");
            if (spring == null) {
                log.warn("No spring configuration found");
                return null;
            }
            
            Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
            if (datasource == null) {
                log.warn("No datasource configuration found");
                return null;
            }
            
            DatabaseConfig dbConfig = new DatabaseConfig();
            String url = String.valueOf(datasource.get("url"));
            if (url == null || url.equals("null")) {
                log.warn("No database URL found");
                return null;
            }
            
            // 解析JDBC URL
            if (url.contains("mysql")) {
                dbConfig.setDbType("mysql");
            } else if (url.contains("postgresql")) {
                dbConfig.setDbType("postgresql");
            } else if (url.contains("oracle")) {
                dbConfig.setDbType("oracle");
            } else if (url.contains("sqlserver")) {
                dbConfig.setDbType("sqlserver");
            }
            
            // 从URL中提取信息
            try {
                // jdbc:mysql://localhost:3306/hibiscus_test
                String[] parts = url.split("://")[1].split(":");  // [localhost, 3306/hibiscus_test]
                String host = parts[0];  // localhost
                String[] portAndDb = parts[1].split("/");  // [3306, hibiscus_test]
                
                dbConfig.setHost(host);
                dbConfig.setPort(portAndDb[0]);  // 3306
                dbConfig.setDatabase(portAndDb[1].split("\\?")[0]);  // hibiscus_test
                dbConfig.setUsername(String.valueOf(datasource.get("username")));
                dbConfig.setPassword(String.valueOf(datasource.get("password")));

                log.info("Successfully parsed database config:");
                log.info("URL: {}",url);
                log.info("Host: {}",dbConfig.getHost());
                log.info("Port: {}",dbConfig.getPort());
                log.info("Database: {}", dbConfig.getDatabase());
                log.info("Username: {}", dbConfig.getUsername());
                
                return dbConfig;
            } catch (Exception e) {
                log.warn("Error parsing database URL: {}", e.getMessage());
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            log.warn("Error loading database config: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private Map<String, Object> loadYamlConfig(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return null;
            }

            Yaml yaml = new Yaml();
            try (FileInputStream fis = new FileInputStream(file)) {
                return yaml.load(fis);
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    public Map<String, Object> checkConnection() {
        DatabaseConfig config = loadDatabaseConfig();
        if (config == null) {
            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put("connected", false);
            hashMap.put("message", "数据库配置未找到");
            return hashMap;
        }
        
        try {
            Class.forName(getDriverClass(config.getDbType()));
            try (Connection conn = DriverManager.getConnection(
                    config.getUrl(), config.getUsername(), config.getPassword())) {
                
                List<Map<String, Object>> tables = getTables(conn);
                Map<String, Object> result = new HashMap<>();
                result.put("connected", true);
                result.put("tables", tables);
                result.put("url", config.getUrl());
                result.put("username", config.getUsername());
                result.put("dbType", config.getDbType());
                return result;
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("connected", false);
            response.put( "message", "连接失败: " + e.getMessage());
            return response;
        }
    }
    
    private List<Map<String, Object>> getTables(Connection conn) throws SQLException {
        List<Map<String, Object>> tables = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();
        String catalog = conn.getCatalog();
        String schema = getSchema(conn);
        
        try (ResultSet rs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (shouldIncludeTable(tableName, conn)) {
                    Map<String, Object> table = new HashMap<>();
                    table.put("name", tableName);
                    table.put("comment", rs.getString("REMARKS"));
                    
                    // 获取表的记录数
                    try (Statement stmt = conn.createStatement();
                         ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                        if (countRs.next()) {
                            table.put("recordCount", countRs.getLong(1));
                        } else {
                            table.put("recordCount", 0L);
                        }
                    } catch (SQLException e) {
                        // 如果获取记录数失败，设置为-1
                        table.put("recordCount", -1L);
                    }
                    
                    tables.add(table);
                }
            }
        }
        return tables;
    }
    
    private String getSchema(Connection conn) throws SQLException {
        String dbType = conn.getMetaData().getDatabaseProductName().toLowerCase();
        switch (dbType) {
            case "postgresql":
                return "public";
            case "oracle":
                return conn.getSchema().toUpperCase();
            case "microsoft sql server":
                return "dbo";
            default:
                return null;
        }
    }
    
    private boolean shouldIncludeTable(String tableName, Connection conn) throws SQLException {
        String dbType = conn.getMetaData().getDatabaseProductName().toLowerCase();
        switch (dbType) {
            case "mysql":
                return !tableName.startsWith("sys_") 
                    && !tableName.startsWith("information_schema")
                    && !tableName.startsWith("performance_schema")
                    && !tableName.startsWith("mysql");
            case "postgresql":
                return !tableName.startsWith("pg_");
            case "oracle":
                return !tableName.startsWith("SYSTEM_")
                    && !tableName.startsWith("SYS_");
            case "microsoft sql server":
                return !tableName.startsWith("sys")
                    && !tableName.startsWith("dt_");
            default:
                return true;
        }
    }
    
    private String getDriverClass(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "postgresql":
                return "org.postgresql.Driver";
            case "oracle":
                return "oracle.jdbc.OracleDriver";
            case "sqlserver":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }
    
    public List<Map<String, Object>> getTableData(String tableName) {
        DatabaseConfig config = loadDatabaseConfig();
        if (config == null) {
            throw new RuntimeException("数据库配置未找到");
        }
        
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Class.forName(getDriverClass(config.getDbType()));
            try (Connection conn = DriverManager.getConnection(
                    config.getUrl(), config.getUsername(), config.getPassword());
                 Statement stmt = conn.createStatement()) {
                
                // 先获取表的主键
                DatabaseMetaData metaData = conn.getMetaData();
                String primaryKey = null;
                try (ResultSet rs = metaData.getPrimaryKeys(conn.getCatalog(), null, tableName)) {
                    if (rs.next()) {
                        primaryKey = rs.getString("COLUMN_NAME");
                    }
                }

                // 获取所有列信息
                List<String> columns = new ArrayList<>();
                try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, tableName, null)) {
                    while (rs.next()) {
                        columns.add(rs.getString("COLUMN_NAME"));
                    }
                }

                // 构建查询SQL
                String sql = "SELECT * FROM " + tableName + " LIMIT 1000";
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    ResultSetMetaData rsMetaData = rs.getMetaData();
                    int columnCount = rsMetaData.getColumnCount();
                    
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        // 如果有主键，确保主键列在第一位
                        if (primaryKey != null) {
                            row.put(primaryKey, rs.getObject(primaryKey));
                        }
                        // 添加其他列
                        for (String column : columns) {
                            if (!column.equals(primaryKey)) {
                                row.put(column, rs.getObject(column));
                            }
                        }
                        data.add(row);
                    }
                }
            }
            return data;
        } catch (Exception e) {
            throw new RuntimeException("获取表数据失败: " + e.getMessage());
        }
    }
    
    public void updateTableData(String tableName, String primaryKey, String primaryKeyValue, Map<String, Object> data) {
        DatabaseConfig config = loadDatabaseConfig();
        if (config == null) {
            throw new RuntimeException("No database configuration found");
        }
        
        try {
            Class.forName(getDriverClass(config.getDbType()));
            try (Connection conn = DriverManager.getConnection(
                    config.getUrl(), config.getUsername(), config.getPassword())) {
                
                StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
                List<Object> values = new ArrayList<>();
                
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (!entry.getKey().equals(primaryKey)) {
                        sql.append(entry.getKey()).append("=?, ");
                        values.add(entry.getValue());
                    }
                }
                
                sql.setLength(sql.length() - 2); // 移除最后的逗号和空格
                sql.append(" WHERE ").append(primaryKey).append("=?");
                values.add(primaryKeyValue);
                
                try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                    for (int i = 0; i < values.size(); i++) {
                        pstmt.setObject(i + 1, values.get(i));
                    }
                    pstmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update table data: " + e.getMessage());
        }
    }
    
    public void deleteTableData(String tableName, String primaryKey, String primaryKeyValue) {
        DatabaseConfig config = loadDatabaseConfig();
        if (config == null) {
            throw new RuntimeException("No database configuration found");
        }
        
        try {
            Class.forName(getDriverClass(config.getDbType()));
            try (Connection conn = DriverManager.getConnection(
                    config.getUrl(), config.getUsername(), config.getPassword());
                 PreparedStatement pstmt = conn.prepareStatement(
                     "DELETE FROM " + tableName + " WHERE " + primaryKey + "=?")) {
                
                pstmt.setObject(1, primaryKeyValue);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete table data: " + e.getMessage());
        }
    }
} 