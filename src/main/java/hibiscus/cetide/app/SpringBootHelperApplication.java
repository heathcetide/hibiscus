package hibiscus.cetide.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"hibiscus.cetide.app"})
@EnableScheduling
public class SpringBootHelperApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootHelperApplication.class, args);
    }
} 