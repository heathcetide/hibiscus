package hibiscus.cetide.app.component.ssh;

public class HibiscusSSHException extends RuntimeException {
    private final String sessionId;

    public HibiscusSSHException(String message, String sessionId) {
        super(message);
        this.sessionId = sessionId;
    }

    public HibiscusSSHException(String message, String sessionId, Throwable cause) {
        super(message, cause);
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}