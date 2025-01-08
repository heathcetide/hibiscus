package hibiscus.cetide.app.component.ssh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HibiscusSSHWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, HibiscusSSHConnector> sshConnectors = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(HibiscusSSHConnector.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private HibiscusSSHProperties sshProperties;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.get("type").asText();

            switch (type) {
                case "connect":
                    handleConnect(session, json);
                    break;
                case "command":
                    handleCommand(session, json);
                    break;
                case "resize":
                    handleResize(session, json);
                    break;
                default:
                    log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
        }
    }

    private void handleConnect(WebSocketSession session, JsonNode json) {
        try {
            String host = json.get("host").asText();
            int port = json.get("port").asInt();
            String username = json.get("username").asText();
            String password = json.get("password").asText();

            // 使用正确的构造函数
            HibiscusSSHConnector connector = new HibiscusSSHConnector(session, sshProperties, session.getId());
            sshConnectors.put(session.getId(), connector);

            // 调用connect方法连接SSH
            connector.connect(host, port, username, password);

        } catch (Exception e) {
            log.error("Failed to establish SSH connection", e);
            try {
                session.sendMessage(new TextMessage("连接失败: " + e.getMessage()));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }

    private void handleCommand(WebSocketSession session, JsonNode json) {
        HibiscusSSHConnector connector = sshConnectors.get(session.getId());
        if (connector != null) {
            connector.sendCommand(json.get("command").asText());
        } else {
            log.warn("No SSH connector found for session: {}", session.getId());
        }
    }

    private void handleResize(WebSocketSession session, JsonNode json) {
        HibiscusSSHConnector connector = sshConnectors.get(session.getId());
        if (connector != null && connector.isConnected()) {
            try {
                int cols = json.get("cols").asInt();
                int rows = json.get("rows").asInt();
                // 使用channel的setPtySize方法
                connector.getChannel().setPtySize(cols, rows, cols * 8, rows * 8);
            } catch (Exception e) {
                log.error("Failed to resize terminal", e);
            }
        } else {
            log.warn("No active SSH connector found for session: {}", session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        HibiscusSSHConnector connector = sshConnectors.remove(session.getId());
        if (connector != null) {
            connector.close();
        }
        log.info("WebSocket connection closed: {}", session.getId());
    }
}