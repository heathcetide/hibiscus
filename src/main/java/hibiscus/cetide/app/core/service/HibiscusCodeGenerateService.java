package hibiscus.cetide.app.core.service;

import hibiscus.cetide.app.core.model.DatabaseConfig;
import hibiscus.cetide.app.core.controller.HibiscusCodeGenerateController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Service
public class HibiscusCodeGenerateService {
    
    private final ITemplateEngine templateEngine;
    private DatabaseConfig currentConfig;
    private Map<String, List<TableColumn>> tableColumns = new HashMap<>();
    private Map<String, String> generatedFiles = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(HibiscusCodeGenerateService.class);


    public HibiscusCodeGenerateService(ITemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
    
    public Map<String, Object> connectAndGetTables(DatabaseConfig config) {
        try {
            Class.forName(getDriverClass(config.getDbType()));
            try (Connection conn = DriverManager.getConnection(
                    config.getUrl(), config.getUsername(), config.getPassword())) {
                
                this.currentConfig = config;
                List<TableInfo> tables = getTables(conn);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("tables", tables);
                return response;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to database: " + e.getMessage());
        }
    }
    
    public Map<String, Object> generateCode(HibiscusCodeGenerateController.GenerateRequest request) {
        if (currentConfig == null) {
            throw new RuntimeException("Please connect to database first");
        }
        
        try (Connection conn = DriverManager.getConnection(
                currentConfig.getUrl(), currentConfig.getUsername(), currentConfig.getPassword())) {
            
            Map<String, String> files = new HashMap<>();
            for (String table : request.getTables()) {
                List<TableColumn> columns = getTableColumns(conn, table);
                tableColumns.put(table, columns);
                
                for (String option : request.getOptions()) {
                    String code = generateFileForTable(option, table, request.getPackageName(), 
                        request.getAuthor(), columns);
                    String filename = getFileName(option, table);
                    files.put(filename, code);
                    generatedFiles.put(filename, code);
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("files", files);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code: " + e.getMessage());
        }
    }
    
    private String generateFileForTable(String option, String table, String packageName, 
            String author, List<TableColumn> columns) {
        Context context = new Context();
        context.setVariable("className", tableNameToClassName(table));
        context.setVariable("packageName", packageName);
        context.setVariable("author", author);
        context.setVariable("tableName", table);
        context.setVariable("fields", columns);
        
        String templateName = "code-templates/" + option;
        return processTemplate(templateName, context);
    }
    
    public byte[] generateZipFile() {
        if (generatedFiles.isEmpty()) {
            throw new RuntimeException("No files have been generated yet");
        }
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create zip file", e);
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
    
    private List<TableInfo> getTables(Connection conn) throws SQLException {
        List<TableInfo> tables = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();
        String catalog = conn.getCatalog();
        String schema = null;
        
        if (currentConfig != null) {
            switch (currentConfig.getDbType().toLowerCase()) {
                case "mysql":
                    schema = null;
                    break;
                case "postgresql":
                    schema = "public";
                    break;
                case "oracle":
                    schema = currentConfig.getUsername().toUpperCase();
                    break;
                case "sqlserver":
                    schema = "dbo";
                    break;
            }
        }
        
        try (ResultSet rs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (shouldIncludeTable(tableName, currentConfig.getDbType())) {
                    TableInfo table = new TableInfo();
                    table.setName(tableName);
                    table.setComment(rs.getString("REMARKS"));
                    if (currentConfig.getDbType().equalsIgnoreCase("mysql") 
                            && (table.getComment() == null || table.getComment().isEmpty())) {
                        table.setComment(getTableComment(conn, tableName));
                    }
                    tables.add(table);
                }
            }
        }
        
        return tables;
    }
    
    private boolean shouldIncludeTable(String tableName, String dbType) {
        switch (dbType.toLowerCase()) {
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
            case "sqlserver":
                return !tableName.startsWith("sys")
                    && !tableName.startsWith("dt_");
            default:
                return true;
        }
    }
    
    private String getTableComment(Connection conn, String tableName) {
        String comment = "";
        String sql = "SELECT TABLE_COMMENT FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conn.getCatalog());
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    comment = rs.getString("TABLE_COMMENT");
                }
            }
        } catch (SQLException e) {
            return "";
        }
        return comment;
    }
    
    private List<TableColumn> getTableColumns(Connection conn, String tableName) throws SQLException {
        List<TableColumn> columns = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();
        String catalog = conn.getCatalog();
        String schema = null;
        
        if (currentConfig != null) {
            switch (currentConfig.getDbType().toLowerCase()) {
                case "mysql":
                    schema = null;
                    break;
                case "postgresql":
                    schema = "public";
                    break;
                case "oracle":
                    schema = currentConfig.getUsername().toUpperCase();
                    break;
                case "sqlserver":
                    schema = "dbo";
                    break;
            }
        }
        
        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        
        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (rs.next()) {
                TableColumn column = new TableColumn();
                String columnName = rs.getString("COLUMN_NAME");
                column.setName(columnName);
                column.setType(getJavaType(rs.getInt("DATA_TYPE")));
                column.setComment(rs.getString("REMARKS"));
                
                if (currentConfig.getDbType().equalsIgnoreCase("mysql") 
                        && (column.getComment() == null || column.getComment().isEmpty())) {
                    column.setComment(getColumnComment(conn, tableName, columnName));
                }
                
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setPrimaryKey(primaryKeys.contains(columnName));
                columns.add(column);
            }
        }
        
        return columns;
    }
    
    private String getColumnComment(Connection conn, String tableName, String columnName) {
        String comment = "";
        String sql = "SELECT COLUMN_COMMENT FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conn.getCatalog());
            stmt.setString(2, tableName);
            stmt.setString(3, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    comment = rs.getString("COLUMN_COMMENT");
                }
            }
        } catch (SQLException e) {
            return "";
        }
        return comment;
    }
    
    private String getJavaType(int sqlType) {
        switch (sqlType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                return "String";
            case Types.NUMERIC:
            case Types.DECIMAL:
                return "BigDecimal";
            case Types.BIT:
                return "Boolean";
            case Types.TINYINT:
                return "Byte";
            case Types.SMALLINT:
                return "Short";
            case Types.INTEGER:
                return "Integer";
            case Types.BIGINT:
                return "Long";
            case Types.REAL:
                return "Float";
            case Types.FLOAT:
            case Types.DOUBLE:
                return "Double";
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return "byte[]";
            case Types.DATE:
                return "Date";
            case Types.TIME:
                return "Time";
            case Types.TIMESTAMP:
                return "Timestamp";
            default:
                return "String";
        }
    }
    
    private String tableNameToClassName(String tableName) {
        tableName = tableName.replaceAll("^(t_|tbl_)", "");
        
        StringBuilder result = new StringBuilder();
        String[] parts = tableName.split("_");
        for (String part : parts) {
            if (part.length() > 0) {
                result.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
    
    private String getFileName(String option, String tableName) {
        String className = tableNameToClassName(tableName);
        switch (option) {
            case "entity":
                return "entity/" + className + ".java";
            case "mapper":
                return "mapper/" + className + "Mapper.java";
            case "mapper-xml":
                return "mapper/xml/" + className + "Mapper.xml";
            case "service":
                return "service/" + className + "Service.java";
            case "service-impl":
                return "service/impl/" + className + "ServiceImpl.java";
            case "controller":
                return "controller/" + className + "Controller.java";
            case "dto-add":
                return "dto/" + className + "AddRequest.java";
            case "dto-edit":
                return "dto/" + className + "EditRequest.java";
            case "dto-query":
                return "dto/" + className + "QueryRequest.java";
            case "vo":
                return "vo/" + className + "VO.java";
            default:
                throw new IllegalArgumentException("Unknown option: " + option);
        }
    }
    
    private String processTemplate(String templateName, Context context) {
        String result = templateEngine.process(templateName, context);
        result = result.replaceAll("<!DOCTYPE.*?>", "")
                      .replaceAll("<html.*?>", "")
                      .replaceAll("</html>", "")
                      .replaceAll("<body>", "")
                      .replaceAll("</body>", "")
                      .replaceAll("<pre>", "")
                      .replaceAll("</pre>", "")
                      .trim();
        return result;
    }
    
    public void applyToProject(Map<String, String> files) {
        String baseDir = System.getProperty("user.dir") + "/src/main/java/";
        
        files.forEach((path, content) -> {
            try {
                // 处理路径
                String fullPath = baseDir + path;
                File file = new File(fullPath);
                
                // 确保目录存在
                file.getParentFile().mkdirs();
                
                // 写入文件
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(content);
                }
                
                log.info("File written: {}", fullPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file: " + path, e);
            }
        });
    }
    
    public static class TableInfo {
        private String name;
        private String comment;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
    

    public static class TableColumn {
        private String name;
        private String type;
        private String comment;
        private boolean nullable;
        private boolean primaryKey;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
        }
    }
} 