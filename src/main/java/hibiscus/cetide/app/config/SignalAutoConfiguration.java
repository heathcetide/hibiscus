package hibiscus.cetide.app.config;

import hibiscus.cetide.app.basic.aspect.SignalAspect;
import hibiscus.cetide.app.component.signal.SignalManager;
import hibiscus.cetide.app.component.signal.DefaultSignalManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

@Configuration
public class SignalAutoConfiguration {

    @Bean(initMethod = "initialize")
    @ConditionalOnMissingBean
    @Primary
    public SignalManager signalManager() {
        return new DefaultSignalManager();
    }

    @Bean
    @DependsOn("signalManager")
    @ConditionalOnMissingBean
    public SignalAspect signalAspect(SignalManager signalManager) {
        return new SignalAspect(signalManager);
    }
}