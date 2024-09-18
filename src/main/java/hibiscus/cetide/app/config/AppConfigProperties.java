package hibiscus.cetide.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@ConfigurationProperties("cetide")
@Configuration
@ComponentScan
public class AppConfigProperties {
    private String hibiscus = "hibiscus.cetide.app";

    public String getHibiscus() {
        return hibiscus;
    }

    public void setHibiscus(String hibiscus) {
        this.hibiscus = hibiscus;
    }

}
