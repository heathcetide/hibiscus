package hibiscus.cetide.app.component.cache.warmup;

public class WarmupAnalysis {
    private final double avgLatency;
    private final double hitRate;
    private final double throughput;
    private final int errors;
    
    public WarmupAnalysis(double avgLatency, double hitRate, double throughput, int errors) {
        this.avgLatency = avgLatency;
        this.hitRate = hitRate;
        this.throughput = throughput;
        this.errors = errors;
    }
    
    public double getAverageLatency() {
        return avgLatency;
    }
    
    public double getHitRate() {
        return hitRate;
    }
    
    public double getThroughput() {
        return throughput;
    }
    
    public int getErrors() {
        return errors;
    }
} 