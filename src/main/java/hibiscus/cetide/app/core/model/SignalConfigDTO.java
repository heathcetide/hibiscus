package hibiscus.cetide.app.core.model;


import hibiscus.cetide.app.component.signal.SignalPriority;

public class SignalConfigDTO {
    private boolean async;
    private int maxRetries;
    private long retryDelayMs;
    private int maxHandlers;
    private long timeoutMs;
    private boolean recordMetrics;
    private SignalPriority priority;
    private String groupName;
    private boolean persistent;

    // Getters and setters
    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public int getMaxHandlers() {
        return maxHandlers;
    }

    public void setMaxHandlers(int maxHandlers) {
        this.maxHandlers = maxHandlers;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isRecordMetrics() {
        return recordMetrics;
    }

    public void setRecordMetrics(boolean recordMetrics) {
        this.recordMetrics = recordMetrics;
    }

    public SignalPriority getPriority() {
        return priority;
    }

    public void setPriority(SignalPriority priority) {
        this.priority = priority;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }
}