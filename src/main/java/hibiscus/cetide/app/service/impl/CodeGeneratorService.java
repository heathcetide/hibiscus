package hibiscus.cetide.app.service.impl;

import hibiscus.cetide.app.core.AppConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Service
public class CodeGeneratorService {

    @Autowired
    private AppConfigProperties appConfigProperties;

    public String generateCode(String className, String packageName, String fields) {
        String BASE_PATH = "src/main/java/"+appConfigProperties.getHibiscus().replace("/", ".")+"/generate/";
        // 生成实体类
        String modelCode = generateModelCode(className, packageName, fields);
        String controllerCode = generateControllerCode(className, packageName);
        String serviceCode = generateServiceCode(className, packageName);
        String mapperCode = generateMapperCode(className, packageName);

        // 写入代码文件
        try {
            writeFile(BASE_PATH + "model/", className, modelCode);
            writeFile(BASE_PATH + "controller/", className + "Controller", controllerCode);
            writeFile(BASE_PATH + "service/", className + "Service", serviceCode);
            writeFile(BASE_PATH + "mapper/", className + "Mapper", mapperCode);
        } catch (IOException e) {
            e.printStackTrace();
            return "代码生成失败: " + e.getMessage();
        }

        return "代码生成成功";
    }

    // 生成实体类代码
    private String generateModelCode(String className, String packageName, String fields) {
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".generate").append(".model;\n\n");
        code.append("public class ").append(className).append(" {\n");

        String[] fieldList = fields.split(",");
        for (String field : fieldList) {
            String[] fieldParts = field.trim().split(" ");
            if (fieldParts.length == 2) {
                String fieldType = fieldParts[0];
                String fieldName = fieldParts[1];
                code.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");
            }
        }
        code.append("\n");

        // 无参和有参构造方法
        code.append("    public ").append(className).append("() {}\n\n");
        code.append("    public ").append(className).append("(");
        for (int i = 0; i < fieldList.length; i++) {
            String[] fieldParts = fieldList[i].trim().split(" ");
            if (fieldParts.length == 2) {
                String fieldType = fieldParts[0];
                String fieldName = fieldParts[1];
                if (i > 0) code.append(", ");
                code.append(fieldType).append(" ").append(fieldName);
            }
        }
        code.append(") {\n");
        for (String field : fieldList) {
            String[] fieldParts = field.trim().split(" ");
            if (fieldParts.length == 2) {
                String fieldName = fieldParts[1];
                code.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
            }
        }
        code.append("    }\n\n");

        // Getter 和 Setter
        for (String field : fieldList) {
            String[] fieldParts = field.trim().split(" ");
            if (fieldParts.length == 2) {
                String fieldType = fieldParts[0];
                String fieldName = fieldParts[1];
                String capitalizedFieldName = capitalize(fieldName);

                code.append("    public ").append(fieldType).append(" get").append(capitalizedFieldName).append("() {\n");
                code.append("        return ").append(fieldName).append(";\n");
                code.append("    }\n\n");

                code.append("    public void set").append(capitalizedFieldName).append("(").append(fieldType).append(" ").append(fieldName).append(") {\n");
                code.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                code.append("    }\n\n");
            }
        }

        // toString 方法
        code.append("    @Override\n");
        code.append("    public String toString() {\n");
        code.append("        return \"").append(className).append("{\" + \n");
        for (int i = 0; i < fieldList.length; i++) {
            String[] fieldParts = fieldList[i].trim().split(" ");
            if (fieldParts.length == 2) {
                String fieldName = fieldParts[1];
                if (i > 0) code.append("                \", ");
                else code.append("                \"");
                code.append(fieldName).append("='\" + ").append(fieldName).append(" + '\\'' +\n");
            }
        }
        code.append("                '}';\n");
        code.append("    }\n");

        code.append("}\n");
        return code.toString();
    }

    // 生成 Controller 代码
    private String generateControllerCode(String className, String packageName) {
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".generate").append(".controller;\n\n");
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import org.springframework.web.bind.annotation.*;\n");
        code.append("import ").append(packageName).append(".generate").append(".service.").append(className).append("Service;\n");
        code.append("import ").append(packageName).append(".generate").append(".model.").append(className).append(";\n\n");
        code.append("@RestController\n");
        code.append("@RequestMapping(\"/").append(className.toLowerCase()).append("\")\n");
        code.append("public class ").append(className).append("Controller {\n\n");
        code.append("    @Autowired\n");
        code.append("    private ").append(className).append("Service ").append(decapitalize(className)).append("Service;\n\n");

        // 增加CRUD方法
        code.append("    @PostMapping\n");
        code.append("    public void add(@RequestBody ").append(className).append(" ").append(decapitalize(className)).append(") {\n");
        code.append("        ").append(decapitalize(className)).append("Service.save(").append(decapitalize(className)).append(");\n");
        code.append("    }\n\n");

        code.append("    @DeleteMapping(\"/{id}\")\n");
        code.append("    public void delete(@PathVariable Long id) {\n");
        code.append("        ").append(decapitalize(className)).append("Service.removeById(id);\n");
        code.append("    }\n\n");

        code.append("    @PutMapping\n");
        code.append("    public void update(@RequestBody ").append(className).append(" ").append(decapitalize(className)).append(") {\n");
        code.append("        ").append(decapitalize(className)).append("Service.updateById(").append(decapitalize(className)).append(");\n");
        code.append("    }\n\n");

        code.append("    @GetMapping(\"/{id}\")\n");
        code.append("    public ").append(className).append(" get(@PathVariable Long id) {\n");
        code.append("        return ").append(decapitalize(className)).append("Service.getById(id);\n");
        code.append("    }\n");

        code.append("}\n");
        return code.toString();
    }

    // 生成 Service 代码
    private String generateServiceCode(String className, String packageName) {
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".generate").append(".service;\n\n");
        code.append("import com.baomidou.mybatisplus.extension.service.IService;\n");
        code.append("import ").append(packageName).append(".generate").append(".model.").append(className).append(";\n\n");
        code.append("public interface ").append(className).append("Service extends IService<").append(className).append("> {\n");
        code.append("}\n");
        return code.toString();
    }

    // 生成 Mapper 代码
    private String generateMapperCode(String className, String packageName) {
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".generate").append(".mapper;\n\n");
        code.append("import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n");
        code.append("import ").append(packageName).append(".generate").append(".model.").append(className).append(";\n\n");
        code.append("public interface ").append(className).append("Mapper extends BaseMapper<").append(className).append("> {\n");
        code.append("}\n");
        return code.toString();
    }

    // 写入文件方法
    private void writeFile(String directoryPath, String className, String codeContent) throws IOException {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();  // 创建目录
        }

        File file = new File(directory, className + ".java");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(codeContent);
        }
    }

    // 首字母大写
    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // 首字母小写
    private String decapitalize(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    public String generateModel(String module) {
        switch (module.toLowerCase()) {
            case "user":
                return generateUserModuleCode();
            case "admin":
//                return generateAdminModuleCode();
            case "stats":
//                return generateStatsModuleCode();
            default:
                return "未知模块，无法生成代码。";
        }
    }

    private String generateUserModuleCode() {
        // 生成实体类 User
        StringBuilder modelBuilder = new StringBuilder();
        modelBuilder.append("package ").append(getPackagePath("model")).append(";\n\n")
                .append("public class User {\n")
                .append("    private Long id;\n")
                .append("    private String username;\n")
                .append("    private String email;\n")
                .append("    // getter and setter methods\n")
                .append("}\n");

        // 生成 UserController
        StringBuilder controllerBuilder = new StringBuilder();
        controllerBuilder.append("package ").append(getPackagePath("controller")).append(";\n\n")
                .append("import ").append(getPackagePath("service")).append(".UserService;\n")
                .append("import ").append(getPackagePath("model")).append(".User;\n")
                .append("import java.util.List;\n\n")
                .append("public class UserController {\n")
                .append("    private final UserService userService;\n\n")
                .append("    public UserController(UserService userService) {\n")
                .append("        this.userService = userService;\n")
                .append("    }\n\n")
                .append("    public List<User> getAllUsers() {\n")
                .append("        return userService.getAllUsers();\n")
                .append("    }\n")
                .append("}\n");

        // 生成 UserService
        StringBuilder serviceBuilder = new StringBuilder();
        serviceBuilder.append("package ").append(getPackagePath("service")).append(";\n\n")
                .append("import ").append(getPackagePath("model")).append(".User;\n")
                .append("import java.util.List;\n\n")
                .append("public interface UserService {\n")
                .append("    List<User> getAllUsers();\n")
                .append("}\n");

        // 生成 UserServiceImpl
        StringBuilder serviceImplBuilder = new StringBuilder();
        serviceImplBuilder.append("package ").append(getPackagePath("service.impl")).append(";\n\n")
                .append("import ").append(getPackagePath("service")).append(".UserService;\n")
                .append("import ").append(getPackagePath("model")).append(".User;\n")
                .append("import java.util.ArrayList;\n")
                .append("import java.util.List;\n\n")
                .append("public class UserServiceImpl implements UserService {\n")
                .append("    @Override\n")
                .append("    public List<User> getAllUsers() {\n")
                .append("        // 业务逻辑\n")
                .append("        return new ArrayList<>();\n")
                .append("    }\n")
                .append("}\n");

        // 生成 UserMapper
        StringBuilder mapperBuilder = new StringBuilder();
        mapperBuilder.append("package ").append(getPackagePath("mapper")).append(";\n\n")
                .append("import ").append(getPackagePath("model")).append(".User;\n")
                .append("import java.util.List;\n\n")
                .append("public interface UserMapper {\n")
                .append("    List<User> selectAllUsers();\n")
                .append("}\n");

        // 调用写入文件方法
        writeGeneratedCodeToFiles("model", "User", modelBuilder.toString());
        writeGeneratedCodeToFiles("controller", "UserController", controllerBuilder.toString());
        writeGeneratedCodeToFiles("service", "UserService", serviceBuilder.toString());
        writeGeneratedCodeToFiles("service.impl", "UserServiceImpl", serviceImplBuilder.toString());
        writeGeneratedCodeToFiles("mapper", "UserMapper", mapperBuilder.toString());

        return  concatenateBuildersWithNewlines(
                modelBuilder,
                controllerBuilder,
                serviceBuilder,
                serviceImplBuilder,
                mapperBuilder
        ).toString();
    }

    // Admin 和 Stats 模块的代码生成可以类似实现，省略...
    public StringBuilder concatenateBuildersWithNewlines(StringBuilder... builders) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < builders.length; i++) {
            result.append(builders[i]);
            if (i < builders.length - 1) {
                result.append("\n\n\n"); // 插入三个换行符
            }
        }

        return result;
    }
    // 写入文件方法，将生成的代码写入到本地文件
    private void writeGeneratedCodeToFiles(String packageName, String className, String codeContent) {
        String basePackagePath = appConfigProperties.getHibiscus().replace(".", "/");
        String fullPath = "src/main/java/" + basePackagePath + "/generate/" + packageName.replace(".", "/") + "/";

        try {
            File directory = new File(fullPath);
            if (!directory.exists()) {
                directory.mkdirs();  // 如果目录不存在，则创建
            }

            File file = new File(directory, className + ".java");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(codeContent);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获取包路径的方法
    private String getPackagePath(String packageName) {
        return appConfigProperties.getHibiscus() + ".generate." + packageName;
    }
}
