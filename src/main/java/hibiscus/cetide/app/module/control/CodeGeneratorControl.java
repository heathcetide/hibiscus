package hibiscus.cetide.app.module.control;

import hibiscus.cetide.app.common.ApiUrlUtil;
import hibiscus.cetide.app.common.AppConfigProperties;
import hibiscus.cetide.app.module.listener.ListenerAspect;
import hibiscus.cetide.app.common.model.CodeGenerationResponse;
import hibiscus.cetide.app.common.model.MethodMetrics;
import hibiscus.cetide.app.module.service.impl.CodeGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/code")
public class CodeGeneratorControl {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @GetMapping("/generate-html")
    public String codeGeneratePage(Model model) {
        model.addAttribute("baseURL", apiUrlUtil.getServerUrl());

        return "codeGenerator";
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

    @PostMapping("/generateModule")
    @ResponseBody
    public CodeGenerationResponse generateModule(@RequestParam(value = "module", required = false) String module) {
        String generatedCode = codeGeneratorService.generateModel(module);
        System.out.println("generatedCode = " + generatedCode);
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

    @GetMapping("/game")
    public String game() {
        return "index";
    }

    @GetMapping("/")
    @ResponseBody
    public String test() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // 获取数据库名称
            String catalog = metaData.getURL().substring(28); // 数据库名称
            if(catalog.contains("?")){
                catalog = catalog.substring(0, catalog.indexOf("?"));
            }
            System.out.println("Database name: " + catalog);
            String schemaPattern = null; // MySQL 中通常不需要指定 schema

            // 获取所有表信息
            ResultSet tablesCheck = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"});
            if (!tablesCheck.isBeforeFirst()) {
                System.out.println("No tables found in the specified database.");
                return "No tables found.";
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
        } catch (SQLException e) {
            System.err.println("Error generating entity class: " + e.getMessage());
            return "Error generating entity class.";
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            // 打印数据库基本信息
            System.out.println("Database Product Name: " + metaData.getDatabaseProductName());
            System.out.println("Database Product Version: " + metaData.getDatabaseProductVersion());
            System.out.println("Driver Name: " + metaData.getDriverName());
            System.out.println("Driver Version: " + metaData.getDriverVersion());
            System.out.println("URL: " + metaData.getURL());
            System.out.println("User: " + metaData.getUserName());
            // 获取数据库名称（catalog）和模式（schema）
            String catalog = metaData.getURL().substring(28); // 提取数据库名称
            if (catalog.contains("?")){
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
                    System.out.println("Table: " + tableName);

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
        } catch (SQLException e) {
            System.err.println("Error getting database info: " + e.getMessage());
        }
        return "Data Retrieved Successfully";
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

    @GetMapping("/generateEntity")
    @ResponseBody
    public String generateEntity() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // 获取数据库名称
            String catalog = metaData.getURL().substring(28); // 数据库名称
            String schemaPattern = null; // MySQL 中通常不需要指定 schema

            // 获取所有表信息
            ResultSet tablesCheck = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"});
            if (!tablesCheck.isBeforeFirst()) {
                System.out.println("No tables found in the specified database.");
                return "No tables found.";
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
                    classContent.append("\n    }\n");
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
        } catch (SQLException e) {
            System.err.println("Error generating entity class: " + e.getMessage());
            return "Error generating entity class.";
        }
        return "Entity classes generated in specified directory.";
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

}


