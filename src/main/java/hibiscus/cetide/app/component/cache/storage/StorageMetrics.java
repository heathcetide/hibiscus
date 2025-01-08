package hibiscus.cetide.app.component.cache.storage;

import java.util.concurrent.atomic.*;
import java.util.concurrent.ConcurrentHashMap;

public class StorageMetrics {
    private final AtomicLong totalCompressedSize = new AtomicLong();
    private final AtomicLong totalOriginalSize = new AtomicLong();
    private final AtomicLong compressionTime = new AtomicLong();
    private final AtomicLong decompressionTime = new AtomicLong();
    private final AtomicInteger compressionCount = new AtomicInteger();
    private final AtomicInteger errorCount = new AtomicInteger();
    private final ConcurrentHashMap<String, AtomicLong> customMetrics = new ConcurrentHashMap<>();
    
    public void recordCompression(long originalSize, long compressedSize, long time) {
        totalOriginalSize.addAndGet(originalSize);
        totalCompressedSize.addAndGet(compressedSize);
        compressionTime.addAndGet(time);
        compressionCount.incrementAndGet();
    }
    
    public void recordDecompression(long compressedSize, long originalSize, long time) {
        decompressionTime.addAndGet(time);
    }
    
    public void recordError() {
        errorCount.incrementAndGet();
    }
    
    public double getCompressionRatio() {
        long original = totalOriginalSize.get();
        return original == 0 ? 1.0 : 
            totalCompressedSize.get() / (double) original;
    }
    
    public double getAverageCompressionTime() {
        int count = compressionCount.get();
        return count == 0 ? 0.0 : 
            compressionTime.get() / (double) count;
    }
    
    public void recordCustomMetric(String name, long value) {
        customMetrics.computeIfAbsent(name, k -> new AtomicLong())
                    .addAndGet(value);
    }
    
    public String getReport() {
        StringBuilder report = new StringBuilder();
        report.append("Storage Metrics Report:\n")
              .append("Compression Ratio: ").append(String.format("%.2f", getCompressionRatio())).append("\n")
              .append("Avg Compression Time: ").append(String.format("%.2f ms", getAverageCompressionTime() / 1_000_000.0)).append("\n")
              .append("Error Count: ").append(errorCount.get()).append("\n");
        
        customMetrics.forEach((name, value) -> 
            report.append(name).append(": ").append(value.get()).append("\n"));
        
        return report.toString();
    }
} 