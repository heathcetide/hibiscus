package hibiscus.cetide.app.module.service.impl;

import cn.hutool.core.io.FileUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.core.Logger;
import hibiscus.cetide.app.common.utils.AppConfigProperties;
import hibiscus.cetide.app.module.control.CodeGeneratorControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CodeGeneratorService {

    @Autowired
    private Configuration freemarkerConfig;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private Logger logger;

    public String generateCode(String className, String packageName, String fields) {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("className", className);
        dataModel.put("packageName", packageName);

        List<Map<String, String>> fieldList = new ArrayList<>();
        String[] fieldArray = fields.split(",");
        for (String field : fieldArray) {
            String[] parts = field.trim().split(" ");
            if (parts.length == 2) {
                Map<String, String> fieldData = new HashMap<>();
                fieldData.put("type", parts[0]);
                fieldData.put("name", parts[1]);
                fieldList.add(fieldData);
            }
        }
        dataModel.put("fields", fieldList);

        try {
            // 生成并写入文件
            generateAndWriteFile("model.ftl", dataModel, "model", className);
            generateAndWriteFile("controller.ftl", dataModel, "controller", className + "Controller");
            generateAndWriteFile("service.ftl", dataModel, "service", className + "Service");
            generateAndWriteFile("mapper.ftl", dataModel, "mapper", className + "Mapper");
            generateAndWriteFile("serviceImpl.ftl", dataModel, "service/impl", className + "ServiceImpl");
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            return "代码生成失败: " + e.getMessage();
        }
        return "代码生成成功";
    }

    private void generateAndWriteFile(String templateName, Map<String, Object> dataModel, String subPackage, String fileName) throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates/baseModules");
        cfg.setDefaultEncoding("UTF-8");
        Template template = cfg.getTemplate(templateName);
        String packagePath = dataModel.get("packageName").toString().replace(".", "/");
        String directoryPath = "src/main/java/" + packagePath + "/generate/" + subPackage + "/";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (Writer fileWriter = new FileWriter(new File(directory, fileName + ".java"))) {
            template.process(dataModel, fileWriter);
        }
    }

    // 首字母大写
    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
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
                .append("import com.baomidou.mybatisplus.annotation.IdType;\n")
                .append("import com.baomidou.mybatisplus.annotation.TableField;\n\n")
                .append("import com.baomidou.mybatisplus.annotation.TableId;\n\n\n")
                .append("import com.baomidou.mybatisplus.annotation.TableLogic;\n\n\n")
                .append("import com.fasterxml.jackson.annotation.JsonIgnore;\n\n\n")
                .append("import java.io.Serializable;\n\n")
                .append("import java.util.Date;\n\n")
                .append("public class User implements Serializable {\n")
                .append("    @TableField(exist = false)\n")
                .append("    private static final long serialVersionUID = 1L;\n\n")
                .append("    @TableId(type = IdType.ASSIGN_ID)\n")
                .append("    private Long id;\n")
                .append("    private String userName;\n")
                .append("    private String email;\n\n")
                .append("    @JsonIgnore\n")
                .append("    private String userPassword;\n")
                .append("    private String unionId;\n\n")
                .append("    private String mpOpenId;\n")
                .append("    private String userAccount;\n")
                .append("    private String userProfile;\n\n")
                .append("    private String userAvatar;\n")
                .append("    private String phone;\n")
                .append("    private String userRole;\n\n")
                .append("    private Date birthday;\n\n")
                .append("    private Integer gender;\n\n\n")
                .append("    private String country;\n")
                .append("    private String address;\n")
                .append("    private Date createTime;\n\n")
                .append("    private Date updateTime;\n")
                .append("    @TableLogic\n")
                .append("    private Integer isDelete;\n\n")
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

        return concatenateBuildersWithNewlines(
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
                result.append("\n\n\n");
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

    public void generateCodeByTable(String packageName, String dataName, String dataKey, String upperDataKey) {
        // 封装生成参数
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("packageName", packageName);
        dataModel.put("dataName", dataName);
        dataModel.put("dataKey", dataKey);
        dataModel.put("upperDataKey", upperDataKey);

        // 生成路径默认值
        String projectPath = System.getProperty("user.dir");

        // 使用类加载器中的模板文件路径
        String inputPath;
        String outputPath;

        try {
            // 1、生成 Controller
            inputPath = "modules/TemplateController.java.ftl";
            outputPath = String.format("%s/src/main/java/%s/generator/controller/%sController.java",
                    projectPath, appConfigProperties.getHibiscus().replace(".", "/"), upperDataKey);
            doGenerate(inputPath, outputPath, dataModel);

            // 2、生成 Service 接口和实现类
            // 生成 Service 接口
            inputPath = "modules/TemplateService.java.ftl";
            outputPath = String.format("%s/src/main/java/%s/generator/service/%sService.java",
                    projectPath, appConfigProperties.getHibiscus().replace(".", "/"), upperDataKey);
            doGenerate(inputPath, outputPath, dataModel);

            // 生成 Service 实现类
            inputPath = "modules/TemplateServiceImpl.java.ftl";
            outputPath = String.format("%s/src/main/java/%s/generator/service/impl/%sServiceImpl.java",
                    projectPath, appConfigProperties.getHibiscus().replace(".", "/"), upperDataKey);
            doGenerate(inputPath, outputPath, dataModel);

            // 生成 Mapper 接口
            inputPath = "modules/TemplateMapper.java.ftl";
            outputPath = String.format("%s/src/main/java/%s/generator/mapper/%sMapper.java",
                    projectPath, appConfigProperties.getHibiscus().replace(".", "/"), upperDataKey);
            doGenerate(inputPath, outputPath, dataModel);

            // 3、生成数据模型封装类（包括 DTO 和 VO）
            // 生成 DTO
            inputPath = "modules/TemplateAddRequest.java.ftl";
            outputPath = String.format("%s/src/main/java/%s/generator/model/dto/%sAddRequest.java",
                    projectPath, appConfigProperties.getHibiscus().replace(".", "/"), upperDataKey);
            doGenerate(inputPath, outputPath, dataModel);

            inputPath = "modules/TemplateQueryRequest.java.ftl";
            outputPath = String.format("%s/src/main/java/%s/generator/model/dto/%sQueryRequest.java",
                    projectPath, appConfigProperties.getHibiscus().replace(".", "/"), upperDataKey);
            doGenerate(inputPath, outputPath, dataModel);

            inputPath = "modules/TemplateEditRequest.java.ftl";
            outputPath = String.format("%s/src/main/java/%s/generator/model/dto/%sEditRequest.java",
                    projectPath, appConfigProperties.getHibiscus().replace(".", "/"), upperDataKey);
            doGenerate(inputPath, outputPath, dataModel);

            inputPath = "modules/TemplateUpdateRequest.java.ftl";
            outputPath = String.format("%s/src/main/java/%s/generator/model/dto/%sUpdateRequest.java",
                    projectPath, appConfigProperties.getHibiscus().replace(".", "/"), upperDataKey);
            doGenerate(inputPath, outputPath, dataModel);

            // 生成 VO
            inputPath = "modules/TemplateVO.java.ftl";
            outputPath = String.format("%s/src/main/java/%s/generator/model/vo/%sVO.java",
                    projectPath, appConfigProperties.getHibiscus().replace(".", "/"), upperDataKey);
            doGenerate(inputPath, outputPath, dataModel);
            logger.log(LogLevel.INFO, "数据库表全量代码生成完成: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 生成文件
     *
     * @param inputPath  模板文件输入路径
     * @param outputPath 输出路径
     * @param model      数据模型
     * @throws IOException
     * @throws TemplateException
     */
    public static void doGenerate(String inputPath, String outputPath, Object model) throws IOException, TemplateException {
        // 创建 Configuration 对象，指定 FreeMarker 版本号
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_29);

        // 设置模板加载路径使用类加载器
        configuration.setClassLoaderForTemplateLoading(CodeGeneratorControl.class.getClassLoader(), "/templates/modules");

        // 设置模板文件使用的字符集
        configuration.setDefaultEncoding("utf-8");

        // 创建模板对象，加载指定模板
        String templateName = new File(inputPath).getName();
        Template template = configuration.getTemplate(templateName);

        // 文件不存在则创建文件和父目录
        if (!FileUtil.exist(outputPath)) {
            FileUtil.touch(outputPath);
        }

        // 生成
        try (Writer out = new FileWriter(outputPath)) {
            template.process(model, out);
            System.out.println("File generated successfully to: " + outputPath);
        }
    }
}
