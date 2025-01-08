package hibiscus.cetide.app.core.model.monitor;

import java.util.List;

public class ThreadMetrics {
    private int activeThreads;      // 活跃线程数
    private int peakThreads;        // 峰值线程数
    private int daemonThreads;      // 守护线程数
    private List<ThreadInfo> threadHistory;  // 线程历史记录


    public static class ThreadInfo {
        private long timestamp;
        private int threadCount;
        private String state;       // 线程状态

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    public int getActiveThreads() {
        return activeThreads;
    }

    public void setActiveThreads(int activeThreads) {
        this.activeThreads = activeThreads;
    }

    public int getPeakThreads() {
        return peakThreads;
    }

    public void setPeakThreads(int peakThreads) {
        this.peakThreads = peakThreads;
    }

    public int getDaemonThreads() {
        return daemonThreads;
    }

    public void setDaemonThreads(int daemonThreads) {
        this.daemonThreads = daemonThreads;
    }

    public List<ThreadInfo> getThreadHistory() {
        return threadHistory;
    }

    public void setThreadHistory(List<ThreadInfo> threadHistory) {
        this.threadHistory = threadHistory;
    }
}