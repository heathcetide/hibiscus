package hibiscus.cetide.app.component.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HibiscusSSHHeartbeatMonitor {

    private final HibiscusSSHSessionManager sessionManager;

    private static Logger log = LoggerFactory.getLogger(HibiscusSSHHeartbeatMonitor.class);

    public HibiscusSSHHeartbeatMonitor(HibiscusSSHSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Scheduled(fixedRate = 30000)
    public void checkSessions() {
        sessionManager.getActiveSessions().forEach((sessionId, connector) -> {
            if (!connector.isConnected()) {
                log.warn("SSH session {} is disconnected, removing...", sessionId);
                sessionManager.removeSession(sessionId);
            }
        });
    }
}