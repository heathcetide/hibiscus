package hibiscus.cetide.app.module.control;

import hibiscus.cetide.app.common.model.ConnectionDetails;
import hibiscus.cetide.app.common.utils.ApiUrlUtil;
import hibiscus.cetide.app.common.utils.AppConfigProperties;
import hibiscus.cetide.app.module.listener.ListenerAspect;
import hibiscus.cetide.app.common.model.CodeGenerationResponse;
import hibiscus.cetide.app.common.model.MethodMetrics;
import hibiscus.cetide.app.module.service.impl.CodeGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/code")
public class CodeGeneratorControl {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

//    @Autowired
//    private DataSource dataSource;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @GetMapping("/generate-html")
    public String codeGeneratePage(Model model) {
        model.addAttribute("baseURL", apiUrlUtil.getServerUrl());
        model.addAttribute("currentDir", "hibiscus.cetide.app");
        return "codeGenerator";
    }

    @GetMapping("/data-manager")
    public String dataManager(Model model) {
        List<Map.Entry<String, ConnectionDetails>> entries = new ArrayList<>(connectionPool.entrySet());
        model.addAttribute("dataSources", entries);
        return "dataManager"; // 返回视图名称
    }

    @GetMapping("/import-module")
    public String importModule(Model model) {
        model.addAttribute("baseURL", apiUrlUtil.getServerUrl());
        return "importModule";
    }

    @PostMapping({"/generate"})
    public String generateCode(
            @RequestParam(value = "className", required = false) String className,
            @RequestParam(value = "packageName", required = false) String packageName,
            @RequestParam(value = "fields", required = false) String fields,
            Model model) {
        String generatedCode = codeGeneratorService.generateCode(className, packageName, fields);
        model.addAttribute("generatedCode", generatedCode);
        return "codeGenerator";
    }


    // 处理全量代码生成请求
    @PostMapping("/generateAllCode")
    public ResponseEntity<Boolean> generateAllCode(@RequestParam String key) {
        try {
            ConnectionDetails connectionDetails = connectionPool.get(key);
            generateEntitiesFromConnection(connectionDetails.getConnection());
            printDatabaseInfo(connectionDetails.getConnection());
            copyResourceFiles();
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
        return ResponseEntity.ok(true);
    }

    // 处理删除数据源请求
    @PostMapping("/deleteDataSource")
    public String deleteDataSource(@RequestParam String key, RedirectAttributes redirectAttributes) {
        try {
            ConnectionDetails remove = connectionPool.remove(key);
            if (remove != null) {
                redirectAttributes.addFlashAttribute("message", "数据源删除成功");
                return "redirect:/code/data-manager";
            } else {
                redirectAttributes.addFlashAttribute("message", "数据源删除失败");
                return "redirect:/code/data-manager";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "数据源删除失败：" + e.getMessage());
            return "redirect:/code/data-manager";
        }
    }


    private ConcurrentHashMap<String, ConnectionDetails> connectionPool = new ConcurrentHashMap<>();

    @PostMapping("/connectDatabase")
    public String connectDatabase(
            @RequestParam String dbType,
            @RequestParam String dbUrl,
            @RequestParam String dbUsername,
            @RequestParam String dbPassword,
            Model model) {

        String driverClassName;

        switch (dbType.toLowerCase()) {
            case "mysql":
                driverClassName = "com.mysql.cj.jdbc.Driver";
                break;
            case "postgresql":
                driverClassName = "org.postgresql.Driver";
                break;
            case "oracle":
                driverClassName = "oracle.jdbc.driver.OracleDriver";
                break;
            default:
                model.addAttribute("message", "不支持的数据库类型！");
                model.addAttribute("connectionPool", connectionPool);
                List<Map.Entry<String, ConnectionDetails>> entries = new ArrayList<>(connectionPool.entrySet());
                model.addAttribute("dataSources", entries);
                return "dataManager";
        }
        try {
            // 加载驱动
            Class.forName(driverClassName);

            // 创建数据库连接
            Connection connection = DriverManager.getConnection(dbUrl+"?serverTimezone=GMT%2B8", dbUsername, dbPassword);

            // 将连接和相关信息存储到连接池中
            ConnectionDetails connectionDetails = new ConnectionDetails(connection, driverClassName, dbType,
                    connection.getMetaData().getURL(), dbUsername, dbPassword);
            System.out.println("Connection created: " + connectionDetails);
            connectionPool.put(dbUrl, connectionDetails); // 保存连接信息

            model.addAttribute("message", "数据库连接成功！");
        } catch (ClassNotFoundException e) {
            model.addAttribute("message", "驱动类未找到：" + e.getMessage());
        } catch (SQLException e) {
            model.addAttribute("message", "数据库连接失败：" + e.getMessage());
        }
        List<Map.Entry<String, ConnectionDetails>> entries = new ArrayList<>(connectionPool.entrySet());
        model.addAttribute("dataSources", entries);
        model.addAttribute("connectionPool", connectionPool);
        return "redirect:/code/data-manager";
    }


    @PostMapping("/executeQuery")
    public String executeQuery(
            @RequestParam String dbUrl,
            @RequestParam String query,
            Model model) {

        ConnectionDetails connection = connectionPool.get(dbUrl);

        if (connection == null) {
            model.addAttribute("message", "未找到对应的数据库连接，请先连接数据库！");
            model.addAttribute("connectionPool", connectionPool);
            return "dataManager";
        }

        try (Statement statement = connection.getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);

            // 处理 ResultSet，转换为结果展示给前端
            StringBuilder result = new StringBuilder();
            while (resultSet.next()) {
                result.append(resultSet.getString(1)).append(" "); // 简单示例
            }
            model.addAttribute("result", result.toString());
        } catch (SQLException e) {
            model.addAttribute("message", "查询执行失败：" + e.getMessage());
        }

        return "dataManager";
    }

    @PostMapping("/generateModule")
    @ResponseBody
    public CodeGenerationResponse generateModule(@RequestParam(value = "module", required = false) String module) {
        String generatedCode = codeGeneratorService.generateModel(module);
        return new CodeGenerationResponse(generatedCode);
    }

    @Autowired
    private ApiUrlUtil apiUrlUtil;
    @Autowired
    private ListenerAspect listenerAspect;

    @GetMapping("/interface-analysis")
    public String interfaceAnalysis(Model model) {
        List<MethodMetrics> methodMetrics = listenerAspect.getMethodMetrics();
        model.addAttribute("methodMetrics", methodMetrics);
        model.addAttribute("baseURL", apiUrlUtil.getServerUrl());
        return "interfaceAnalysis";
    }

    @GetMapping("/method-metrics")
    @ResponseBody
    public List<MethodMetrics> getMethodMetrics() {
        return listenerAspect.getMethodMetrics();
    }

    @GetMapping("/method-metrics/{methodName}")
    public ResponseEntity<List<Long>> getMethodExecutionHistory(@PathVariable String methodName) {
        MethodMetrics metrics = listenerAspect.getMethodMetrics().stream()
                .filter(m -> m.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);

        if (metrics != null) {
            List<Long> executionTimes = metrics.getExecutionTimeHistory();
            System.out.println("Execution times for " + methodName + ": " + executionTimes);
            return ResponseEntity.ok(executionTimes);
        }
        return ResponseEntity.notFound().build();
    }

    private static final Map<String, String> sqlToJavaTypeMap = new HashMap<>();

    static {
        sqlToJavaTypeMap.put("BIGINT", "Long");
        sqlToJavaTypeMap.put("INT", "Integer");
        sqlToJavaTypeMap.put("VARCHAR", "String");
        sqlToJavaTypeMap.put("DATETIME", "java.time.LocalDateTime");
        sqlToJavaTypeMap.put("TINYINT", "Boolean");
        sqlToJavaTypeMap.put("JSON", "String");
        // 添加其他 SQL 数据类型到 Java 类型的映射
    }

    private String toCamelCase(String input, boolean capitalizeFirstLetter) {
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;

        for (char c : input.toCharArray()) {
            if (c == '_') {
                nextUpperCase = true;
            } else if (nextUpperCase) {
                result.append(Character.toUpperCase(c));
                nextUpperCase = false;
            } else {
                result.append(capitalizeFirstLetter ? Character.toUpperCase(c) : c);
                capitalizeFirstLetter = false;
            }
        }

        return result.toString();
    }

    private String convertToCamelCase(String name, boolean capitalizeFirst) {
        // 检查是否已经是小驼峰命名格式
        if (isCamelCase(name) && !capitalizeFirst) {
            return name;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = capitalizeFirst;
        boolean prevWasUnderscore = false;

        for (char c : name.toCharArray()) {
            if (c == '_') {
                prevWasUnderscore = true;
            } else {
                if (prevWasUnderscore || capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                    prevWasUnderscore = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        return result.toString();
    }

    private boolean isCamelCase(String name) {
        // 检查是否已经是小驼峰命名格式
        if (name.isEmpty() || name.contains("_")) {
            return false;
        }
        return Character.isLowerCase(name.charAt(0)) && name.substring(1).matches("[a-zA-Z0-9]*");
    }


    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }


    private void generateEntitiesFromConnection(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        // 获取数据库名称
        String catalog = metaData.getURL().substring(28); // 数据库名称
        if (catalog.contains("?")) {
            catalog = catalog.substring(0, catalog.indexOf("?"));
        }
        System.out.println("Database name: " + catalog);
        String schemaPattern = null; // MySQL 中通常不需要指定 schema

        // 获取所有表信息
        ResultSet tablesCheck = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"});
        if (!tablesCheck.isBeforeFirst()) {
            System.out.println("No tables found in the specified database.");
        } else {
            while (tablesCheck.next()) {
                String tableName = tablesCheck.getString("TABLE_NAME");

                // 获取类名
                String className = convertToCamelCase(tableName, true);
                StringBuilder classContent = new StringBuilder();

                // 添加包名
                classContent.append("package ").append(appConfigProperties.getHibiscus()).append(".generator.model.entity;\n\n");

                // 添加导入语句
                classContent.append("import java.time.LocalDateTime;\n\n");

                // 添加类声明
                classContent.append("public class ").append(className).append(" {\n");

                // 用于保存字段信息，以便生成 getter、setter 和 toString
                Map<String, String> fields = new HashMap<>();

                // 获取字段信息
                ResultSet columnsResultSet = metaData.getColumns(catalog, schemaPattern, tableName, "%");
                while (columnsResultSet.next()) {
                    String columnName = columnsResultSet.getString("COLUMN_NAME");
                    String dataType = columnsResultSet.getString("TYPE_NAME");
                    int columnSize = columnsResultSet.getInt("COLUMN_SIZE");
                    String isNullable = columnsResultSet.getString("IS_NULLABLE");

                    // 映射 SQL 数据类型到 Java 数据类型
                    String javaType = sqlToJavaTypeMap.getOrDefault(dataType, "String");
                    String fieldName = convertToCamelCase(columnName, false); // 使用小驼峰命名法

                    // 保存字段信息
                    fields.put(fieldName, javaType);

                    // 字段定义和注释
                    classContent.append("    // Column Size: ").append(columnSize).append(", Nullable: ").append(isNullable).append("\n");
                    classContent.append("    private ").append(javaType).append(" ").append(fieldName).append(";\n");
                }

                // 生成 getter 和 setter 方法
                for (Map.Entry<String, String> fieldEntry : fields.entrySet()) {
                    String fieldName = fieldEntry.getKey();
                    String javaType = fieldEntry.getValue();

                    // 生成 getter 方法
                    String getterMethod = "get" + capitalize(fieldName);
                    classContent.append("\n    public ").append(javaType).append(" ").append(getterMethod).append("() {\n");
                    classContent.append("        return ").append(fieldName).append(";\n    }\n");

                    // 生成 setter 方法
                    String setterMethod = "set" + capitalize(fieldName);
                    classContent.append("\n    public void ").append(setterMethod).append("(").append(javaType).append(" ").append(fieldName).append(") {\n");
                    classContent.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n    }\n");
                }
                classContent.append("\n}\n");
                // 将内容写入文件
                String outputDir = System.getProperty("user.dir") + "/src/main/java/" + appConfigProperties.getHibiscus().replace(".", "/") + "/generator/model/entity";
                File directory = new File(outputDir);
                if (!directory.exists()) directory.mkdirs();

                File file = new File(directory, className + ".java");
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(classContent.toString());
                    System.out.println("Generated file: " + file.getPath());
                } catch (IOException e) {
                    System.err.println("Error writing file: " + e.getMessage());
                }
            }
        }
    }

    private void printDatabaseInfo(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        // 获取数据库名称（catalog）和模式（schema）
        String catalog = metaData.getURL().substring(28); // 提取数据库名称
        if (catalog.contains("?")) {
            catalog = catalog.substring(0, catalog.indexOf("?"));
        }
        String schemaPattern = null; // MySQL 中通常不需要指定 schema
        // 获取所有表信息
        ResultSet tablesCheck = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"});
        if (!tablesCheck.isBeforeFirst()) {
            System.out.println("No tables found in the specified database.");
        } else {
            while (tablesCheck.next()) {
                String tableName = tablesCheck.getString("TABLE_NAME");

                // 获取字段信息
                ResultSet columnsResultSet = metaData.getColumns(catalog, schemaPattern, tableName, "%");
                while (columnsResultSet.next()) {
                    String columnName = columnsResultSet.getString("COLUMN_NAME");
                    String dataType = columnsResultSet.getString("TYPE_NAME");
                    int columnSize = columnsResultSet.getInt("COLUMN_SIZE");
                    String isNullable = columnsResultSet.getString("IS_NULLABLE");
                    System.out.println("  Column Name: " + columnName);
                    System.out.println("    Data Type: " + dataType + " (" + columnSize + ")");
                    System.out.println("    Nullable: " + isNullable);
                }

                // 转换表名和字段名
                String packageName = appConfigProperties.getHibiscus();
                String dataName = "数据";
                String dataKey = toCamelCase(tableName, false); // 小驼峰命名
                String upperDataKey = toCamelCase(tableName, true); // 大驼峰命名

                // 生成代码
                codeGeneratorService.generateCodeByTable(packageName, dataName, dataKey, upperDataKey);
            }
        }
    }

    private void copyResourceFiles() {
        codeGeneratorService.copyFileFromResources("PageRequest.java");
        codeGeneratorService.copyFileFromResources("CommonConstant.java");
        codeGeneratorService.copyFileFromResources("AuthCheck.java");
        codeGeneratorService.copyFileFromResources("BaseResponse.java");
        codeGeneratorService.copyFileFromResources("BusinessException.java");
        codeGeneratorService.copyFileFromResources("DeleteRequest.java");
        codeGeneratorService.copyFileFromResources("ErrorCode.java");
        codeGeneratorService.copyFileFromResources("GlobalExceptionHandler.java");
        codeGeneratorService.copyFileFromResources("ResultUtils.java");
        codeGeneratorService.copyFileFromResources("ThrowUtils.java");
        codeGeneratorService.copyFileFromResources("UserConstant.java");
        codeGeneratorService.copyFileFromResources("SqlUtils.java");
    }
}


