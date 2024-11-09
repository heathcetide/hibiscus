package hibiscus.cetide.app;

import hibiscus.cetide.app.common.ApiUrlUtil;
import hibiscus.cetide.app.module.service.ApiMonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class EnvironmentTest {

    @Resource
    private ApiUrlUtil apiUrlUtil;

    @Test
    public void testEnvironment() {
        String environmentName = apiUrlUtil.buildApiUrl("");
        System.out.println(environmentName);
    }
}
