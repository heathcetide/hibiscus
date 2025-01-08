package hibiscus.cetide.app.core.model.monitor;

import java.util.List;

public class ApiMetrics {
    private long totalRequests;
    private double avgResponseTime;
    private double errorRate;
    private List<ApiInfo> apiHistory;
    private List<PathStats> pathStats;


    public static class ApiInfo {
        private long timestamp;
        private String path;
        private long responseTime;
        private int statusCode;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public long getResponseTime() {
            return responseTime;
        }

        public void setResponseTime(long responseTime) {
            this.responseTime = responseTime;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }
    }


    public static class PathStats {
        private String path;
        private long totalRequests;
        private double avgResponseTime;
        private double errorRate;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public long getTotalRequests() {
            return totalRequests;
        }

        public void setTotalRequests(long totalRequests) {
            this.totalRequests = totalRequests;
        }

        public double getAvgResponseTime() {
            return avgResponseTime;
        }

        public void setAvgResponseTime(double avgResponseTime) {
            this.avgResponseTime = avgResponseTime;
        }

        public double getErrorRate() {
            return errorRate;
        }

        public void setErrorRate(double errorRate) {
            this.errorRate = errorRate;
        }
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public double getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    public List<ApiInfo> getApiHistory() {
        return apiHistory;
    }

    public void setApiHistory(List<ApiInfo> apiHistory) {
        this.apiHistory = apiHistory;
    }

    public List<PathStats> getPathStats() {
        return pathStats;
    }

    public void setPathStats(List<PathStats> pathStats) {
        this.pathStats = pathStats;
    }
}