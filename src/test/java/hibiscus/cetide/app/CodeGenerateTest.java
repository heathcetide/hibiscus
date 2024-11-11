package hibiscus.cetide.app;

import hibiscus.cetide.app.module.service.impl.CodeGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CodeGenerateTest {
    @Autowired
    private CodeGeneratorService codeGeneratorService;

    @Test
    public void test() {
//        String result = codeGeneratorService.generateCode("User", "com.example", "String name, int age, boolean active");
//        System.out.println(result);
    }

    @Test
    public void pathTest(){
        // 获取项目根路径
        String currentDir = System.getProperty("user.dir");
        // 设置包名并构建完整路径
        String basePackagePath = "com.example.generated".replace(".", "/");
        String fullPath = currentDir + "/src/main/java/" + basePackagePath;
        // 输出结果
        System.out.println("Full Java Path: " + fullPath);
    }
}
