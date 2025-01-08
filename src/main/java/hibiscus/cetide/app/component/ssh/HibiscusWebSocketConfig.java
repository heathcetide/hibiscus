package hibiscus.cetide.app.component.ssh;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class HibiscusWebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sshWebSocketHandler(), "/webssh")
                .setAllowedOrigins("*");
    }

    @Bean
    public HibiscusSSHWebSocketHandler sshWebSocketHandler() {
        return new HibiscusSSHWebSocketHandler();
    }
}