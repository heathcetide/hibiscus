package hibiscus.cetide.app.core.model;

import java.util.ArrayList;
import java.util.List;

public class MethodMetrics {
    private String methodName;
    private int accessCount;
    private long totalResponseTime;
    private long uploadBytes;
    private long downloadBytes;

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setAccessCount(int accessCount) {
        this.accessCount = accessCount;
    }

    public void setTotalResponseTime(long totalResponseTime) {
        this.totalResponseTime = totalResponseTime;
    }

    public long getUploadBytes() {
        return uploadBytes;
    }

    public void setUploadBytes(long uploadBytes) {
        this.uploadBytes = uploadBytes;
    }

    public long getDownloadBytes() {
        return downloadBytes;
    }

    public void setDownloadBytes(long downloadBytes) {
        this.downloadBytes = downloadBytes;
    }

    public MethodMetrics(String methodName) {
        this.methodName = methodName;
        this.accessCount = 0;
        this.totalResponseTime = 0L;
        this.uploadBytes = 0L;
        this.downloadBytes = 0L;
    }
    public List<Long> getExecutionTimeHistory() {
        return executionTimeHistory;
    }
    public void incrementAccessCount(long executionTime) {
        this.accessCount++;
        this.totalResponseTime += executionTime;
        this.executionTimeHistory.add(executionTime); // 添加到历史记录
    }
    public void incrementUploadBytes(long bytes) {
        this.uploadBytes += bytes;
    }
    public void incrementDownloadBytes(long bytes) {
        this.downloadBytes += bytes;
    }
    public String getMethodName() {
        return methodName;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public double getTotalResponseTime() {
        return totalResponseTime;
    }

    public double getAverageResponseTime() {
        if (accessCount == 0) {
            return 0.0;
        }
        return totalResponseTime / accessCount;
    }

    // 增加一个历史执行时间记录
    private List<Long> executionTimeHistory = new ArrayList<>();
}
