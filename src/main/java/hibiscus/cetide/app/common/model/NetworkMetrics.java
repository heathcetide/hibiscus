package hibiscus.cetide.app.common.model;

public class NetworkMetrics {
    private String methodName;
    private long uploadBytes;
    private long downloadBytes;

    public NetworkMetrics(String methodName) {
        this.methodName = methodName;
        this.uploadBytes = 0L;
        this.downloadBytes = 0L;
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

    public long getUploadBytes() {
        return uploadBytes;
    }

    public long getDownloadBytes() {
        return downloadBytes;
    }
}
