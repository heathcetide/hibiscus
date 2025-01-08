package hibiscus.cetide.app.component.ssh;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HibiscusSSHSessionManager {
    private final Map<String, HibiscusSSHConnector> activeSessions = new ConcurrentHashMap<>();

    public void addSession(String sessionId, HibiscusSSHConnector connector) {
        activeSessions.put(sessionId, connector);
    }

    public HibiscusSSHConnector getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public void removeSession(String sessionId) {
        HibiscusSSHConnector connector = activeSessions.remove(sessionId);
        if (connector != null) {
            connector.close();
        }
    }

    public Map<String, HibiscusSSHConnector> getActiveSessions() {
        return activeSessions;
    }
}