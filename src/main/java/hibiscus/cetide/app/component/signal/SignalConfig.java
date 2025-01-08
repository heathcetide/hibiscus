package hibiscus.cetide.app.component.signal;

public class SignalConfig {
    private boolean async;
    private int maxRetries;
    private long retryDelayMs;
    private int maxHandlers;
    private long timeoutMs;
    private boolean recordMetrics;
    private SignalPriority priority;
    private String groupName;
    private boolean persistent;

    private SignalConfig(Builder builder) {
        this.async = builder.async;
        this.maxRetries = builder.maxRetries;
        this.retryDelayMs = builder.retryDelayMs;
        this.maxHandlers = builder.maxHandlers;
        this.timeoutMs = builder.timeoutMs;
        this.recordMetrics = builder.recordMetrics;
        this.priority = builder.priority;
        this.groupName = builder.groupName;
        this.persistent = builder.persistent;
    }

    public static class Builder {
        private boolean async = false;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
        private int maxHandlers = 100;
        private long timeoutMs = 5000;
        private boolean recordMetrics = true;
        private SignalPriority priority = SignalPriority.MEDIUM;
        private String groupName = null;
        private boolean persistent = false;

        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder retryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
            return this;
        }

        public Builder maxHandlers(int maxHandlers) {
            this.maxHandlers = maxHandlers;
            return this;
        }

        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder recordMetrics(boolean recordMetrics) {
            this.recordMetrics = recordMetrics;
            return this;
        }

        public Builder priority(SignalPriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder persistent(boolean persistent) {
            this.persistent = persistent;
            return this;
        }

        public SignalConfig build() {
            return new SignalConfig(this);
        }
    }

    public boolean isAsync() {
        return async;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public int getMaxHandlers() {
        return maxHandlers;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isRecordMetrics() {
        return recordMetrics;
    }

    public SignalPriority getPriority() {
        return priority;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isPersistent() {
        return persistent;
    }
} 