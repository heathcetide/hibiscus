package hibiscus.cetide.app;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class ApiDocGeneratorTest {

//    @Test
//    public void Test() throws Exception {
        // 解析Java文件，提取类和方法信息
//        File sourceFile = new File("src/main/java/hibiscus/cetide/app/module/control/BaseUserControl.java");
//        CompilationUnit cu = StaticJavaParser.parse(sourceFile);
//
//        // 准备数据模型
//        Map<String, Object> dataModel = new HashMap<>();
//        List<Map<String, Object>> methodsData = new ArrayList<>();
//
//        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
//            dataModel.put("className", classDecl.getNameAsString());
//            dataModel.put("classDescription", classDecl.getComment().map(comment -> comment.getContent()).orElse("No description"));
//
//            for (MethodDeclaration method : classDecl.getMethods()) {
//                Map<String, Object> methodData = new HashMap<>();
//                methodData.put("name", method.getNameAsString());
//                methodData.put("description", method.getComment().map(comment -> comment.getContent()).orElse("No description"));
//                methodData.put("returnType", method.getTypeAsString());
//
//                List<Map<String, String>> paramsData = new ArrayList<>();
//                method.getParameters().forEach(param -> {
//                    Map<String, String> paramData = new HashMap<>();
//                    paramData.put("name", param.getNameAsString());
//                    paramData.put("type", param.getTypeAsString());
//                    paramData.put("description", "No description"); // 可根据需要手动填写或自动获取
//                    paramsData.add(paramData);
//                });
//
//                methodData.put("parameters", paramsData);
//                methodsData.add(methodData);
//            }
//        }
//        dataModel.put("methods", methodsData);
//
//        // 配置 FreeMarker
//        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
//        cfg.setDirectoryForTemplateLoading(new File("src/main/resources/templates/baseModules"));
//        cfg.setDefaultEncoding("UTF-8");
//
//        // 加载模板
//        Template template = cfg.getTemplate("api_documentation_template.md.ftl");
//
//        // 输出到 .md 文件
//        try (Writer fileWriter = new FileWriter(Paths.get("API_Documentation.md").toFile())) {
//            template.process(dataModel, fileWriter);
//        } catch (TemplateException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("API Documentation generated as API_Documentation.md");
//    }
}
