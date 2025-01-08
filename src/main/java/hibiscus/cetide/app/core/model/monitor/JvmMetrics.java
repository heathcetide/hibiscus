package hibiscus.cetide.app.core.model.monitor;

public class JvmMetrics {
    private double heapUsage;        // 堆内存使用率
    private double nonHeapUsage;     // 非堆内存使用率
    private long youngGcCount;       // Young GC次数
    private long fullGcCount;        // Full GC次数
    private long heapUsed;           // 已使用堆内存(bytes)
    private long heapMax;            // 最大堆内存(bytes)
    private long nonHeapUsed;        // 已使用非堆内存(bytes)
    private long nonHeapMax;         // 最大非堆内存(bytes)

    public double getHeapUsage() {
        return heapUsage;
    }

    public void setHeapUsage(double heapUsage) {
        this.heapUsage = heapUsage;
    }

    public double getNonHeapUsage() {
        return nonHeapUsage;
    }

    public void setNonHeapUsage(double nonHeapUsage) {
        this.nonHeapUsage = nonHeapUsage;
    }

    public long getYoungGcCount() {
        return youngGcCount;
    }

    public void setYoungGcCount(long youngGcCount) {
        this.youngGcCount = youngGcCount;
    }

    public long getFullGcCount() {
        return fullGcCount;
    }

    public void setFullGcCount(long fullGcCount) {
        this.fullGcCount = fullGcCount;
    }

    public long getHeapUsed() {
        return heapUsed;
    }

    public void setHeapUsed(long heapUsed) {
        this.heapUsed = heapUsed;
    }

    public long getHeapMax() {
        return heapMax;
    }

    public void setHeapMax(long heapMax) {
        this.heapMax = heapMax;
    }

    public long getNonHeapUsed() {
        return nonHeapUsed;
    }

    public void setNonHeapUsed(long nonHeapUsed) {
        this.nonHeapUsed = nonHeapUsed;
    }

    public long getNonHeapMax() {
        return nonHeapMax;
    }

    public void setNonHeapMax(long nonHeapMax) {
        this.nonHeapMax = nonHeapMax;
    }
}