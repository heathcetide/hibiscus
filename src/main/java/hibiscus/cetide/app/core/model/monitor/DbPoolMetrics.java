package hibiscus.cetide.app.core.model.monitor;

import java.util.List;

public class DbPoolMetrics {
    private int activeConnections;    // 活跃连接数
    private int idleConnections;      // 空闲连接数
    private int maxConnections;       // 最大连接数
    private List<PoolInfo> connectionHistory;  // 连接池历史记录

    public static class PoolInfo {
        private long timestamp;
        private int activeCount;
        private int idleCount;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public int getActiveCount() {
            return activeCount;
        }

        public void setActiveCount(int activeCount) {
            this.activeCount = activeCount;
        }

        public int getIdleCount() {
            return idleCount;
        }

        public void setIdleCount(int idleCount) {
            this.idleCount = idleCount;
        }
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(int activeConnections) {
        this.activeConnections = activeConnections;
    }

    public int getIdleConnections() {
        return idleConnections;
    }

    public void setIdleConnections(int idleConnections) {
        this.idleConnections = idleConnections;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public List<PoolInfo> getConnectionHistory() {
        return connectionHistory;
    }

    public void setConnectionHistory(List<PoolInfo> connectionHistory) {
        this.connectionHistory = connectionHistory;
    }
}