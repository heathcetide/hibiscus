package hibiscus.cetide.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("cetide")
@Configuration
@ComponentScan
public class AppConfigProperties {
    private String hibiscus = "hibiscus.cetide.app";

    private String username = "admin";

    private String password = "admin";

    public String getHibiscus() {
        return hibiscus;
    }

    public void setHibiscus(String hibiscus) {
        this.hibiscus = hibiscus;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
