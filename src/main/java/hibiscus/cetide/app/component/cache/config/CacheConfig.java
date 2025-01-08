package hibiscus.cetide.app.component.cache.config;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CacheConfig<K, V> {
    private final int maxSize;
    private final long defaultTTL;
    private final int cleanupIntervalSeconds;
    private final EvictionPolicy evictionPolicy;
    private final boolean enableMetrics;
    private final long maxMemory;
    private final int concurrencyLevel;
    private final boolean enableWarmup;
    private final boolean asyncWrite;
    private final int writeBufferSize;
    private final double bloomFilterFpp;
    private final boolean enablePrediction;
    private final int predictionThreshold;
    private final boolean useMemoryPool;
    private final int memoryPoolSize;
    private final boolean compressionEnabled;
    private final int compressionThreshold;
    private final int compressionLevel;
    private final boolean multiLevelEnabled;
    private final boolean partitionEnabled;
    private final int partitionCount;
    private final boolean performanceMonitorEnabled;
    private final int monitoringWindowSize;
    private final TimeUnit monitoringWindowUnit;
    private final boolean autoTuneEnabled;
    private final boolean hotspotPreloadEnabled;
    private final int preloadThreads;
    private final int preloadThreshold;
    private final int shardCount;
    private final Function<K, V> loader;
    private final int batchSize;
    private final int batchQueueCapacity;
    private final int batchThreads;
    private final int transactionMaxRetries;
    private final long transactionTimeout;
    private final boolean prefetchEnabled;
    private final int prefetchPatternLength;
    private final int prefetchThreads;
    private final double prefetchThreshold;
    private final boolean adaptiveEnabled = true;
    private final boolean snapshotEnabled = true;
    private final int indexSegments = 16;
    private final int indexBuckets = 256;
    private final long indexOptimizationInterval = 60000;
    private final boolean tieredStorageEnabled = true;
    private final long heapStorageSize = 100_000;
    private final long directStorageSize = 1_000_000;
    private final long diskStorageSize = 10_000_000;
    private final int storageMigrationThreads = 4;
    private final int maxPoolSize = 1000;
    private final int warmupThreads = 4;
    private final int warmupBatchSize = 1000;

    public enum EvictionPolicy {
        LRU,    // Least Recently Used
        LFU,    // Least Frequently Used
        FIFO,   // First In First Out
        W_TINYLFU // Window TinyLFU
    }

    private CacheConfig(Builder<K, V> builder) {
        this.maxSize = builder.maxSize;
        this.defaultTTL = builder.defaultTTL;
        this.cleanupIntervalSeconds = builder.cleanupIntervalSeconds;
        this.evictionPolicy = builder.evictionPolicy;
        this.enableMetrics = builder.enableMetrics;
        this.maxMemory = builder.maxMemory;
        this.concurrencyLevel = builder.concurrencyLevel;
        this.enableWarmup = builder.enableWarmup;
        this.asyncWrite = builder.asyncWrite;
        this.writeBufferSize = builder.writeBufferSize;
        this.bloomFilterFpp = builder.bloomFilterFpp;
        this.enablePrediction = builder.enablePrediction;
        this.predictionThreshold = builder.predictionThreshold;
        this.useMemoryPool = builder.useMemoryPool;
        this.memoryPoolSize = builder.memoryPoolSize;
        this.compressionEnabled = builder.compressionEnabled;
        this.compressionThreshold = builder.compressionThreshold;
        this.compressionLevel = builder.compressionLevel;
        this.multiLevelEnabled = builder.multiLevelEnabled;
        this.partitionEnabled = builder.partitionEnabled;
        this.partitionCount = builder.partitionCount;
        this.performanceMonitorEnabled = builder.performanceMonitorEnabled;
        this.monitoringWindowSize = builder.monitoringWindowSize;
        this.monitoringWindowUnit = builder.monitoringWindowUnit;
        this.autoTuneEnabled = builder.autoTuneEnabled;
        this.hotspotPreloadEnabled = builder.hotspotPreloadEnabled;
        this.preloadThreads = builder.preloadThreads;
        this.preloadThreshold = builder.preloadThreshold;
        this.shardCount = builder.shardCount;
        this.loader = builder.loader;
        this.batchSize = builder.batchSize;
        this.batchQueueCapacity = builder.batchQueueCapacity;
        this.batchThreads = builder.batchThreads;
        this.transactionMaxRetries = builder.transactionMaxRetries;
        this.transactionTimeout = builder.transactionTimeout;
        this.prefetchEnabled = builder.prefetchEnabled;
        this.prefetchPatternLength = builder.prefetchPatternLength;
        this.prefetchThreads = builder.prefetchThreads;
        this.prefetchThreshold = builder.prefetchThreshold;
    }

    public static class Builder<K, V> {
        private int maxSize = 10000;
        private long defaultTTL = TimeUnit.MINUTES.toMillis(30);
        private int cleanupIntervalSeconds = 1000;
        private EvictionPolicy evictionPolicy = EvictionPolicy.LRU;
        private boolean enableMetrics = true;
        private long maxMemory = Runtime.getRuntime().maxMemory() / 4;
        private int concurrencyLevel = Runtime.getRuntime().availableProcessors() * 4;
        private boolean enableWarmup = true;
        private boolean asyncWrite = true;
        private int writeBufferSize = 1024;
        private double bloomFilterFpp = 0.01;
        private boolean enablePrediction = true;
        private int predictionThreshold = 10;
        private boolean useMemoryPool = true;
        private int memoryPoolSize = 1000;
        private boolean compressionEnabled = true;
        private int compressionThreshold = 1024;
        private int compressionLevel = 6;
        private boolean multiLevelEnabled = false;
        private boolean partitionEnabled = false;
        private int partitionCount = 16;
        private boolean performanceMonitorEnabled = true;
        private int monitoringWindowSize = 60;
        private TimeUnit monitoringWindowUnit = TimeUnit.SECONDS;
        private boolean autoTuneEnabled = true;
        private boolean hotspotPreloadEnabled = true;
        private int preloadThreads = Runtime.getRuntime().availableProcessors();
        private int preloadThreshold = 1000;
        private int shardCount = 16;
        private Function<K, V> loader;
        private int batchSize = 1000;
        private int batchQueueCapacity = 10000;
        private int batchThreads = Runtime.getRuntime().availableProcessors();
        private int transactionMaxRetries = 3;
        private long transactionTimeout = TimeUnit.SECONDS.toMillis(10);
        private boolean prefetchEnabled = true;
        private int prefetchPatternLength = 5;
        private int prefetchThreads = Runtime.getRuntime().availableProcessors();
        private double prefetchThreshold = 0.7;
        private boolean useDirectMemory = true;

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder defaultTTL(long ttl, TimeUnit unit) {
            this.defaultTTL = unit.toMillis(ttl);
            return this;
        }

        public Builder cleanupInterval(int interval, TimeUnit unit) {
            this.cleanupIntervalSeconds = (int) unit.toSeconds(interval);
            return this;
        }

        public Builder evictionPolicy(EvictionPolicy policy) {
            this.evictionPolicy = policy;
            return this;
        }

        public Builder enableMetrics(boolean enable) {
            this.enableMetrics = enable;
            return this;
        }

        public Builder maxMemory(long maxMemory) {
            this.maxMemory = maxMemory;
            return this;
        }

        public Builder concurrencyLevel(int level) {
            this.concurrencyLevel = level;
            return this;
        }

        public Builder enableWarmup(boolean enable) {
            this.enableWarmup = enable;
            return this;
        }

        public Builder asyncWrite(boolean async) {
            this.asyncWrite = async;
            return this;
        }

        public Builder writeBufferSize(int size) {
            this.writeBufferSize = size;
            return this;
        }

        public Builder bloomFilterFpp(double fpp) {
            this.bloomFilterFpp = fpp;
            return this;
        }

        public Builder enablePrediction(boolean enable) {
            this.enablePrediction = enable;
            return this;
        }

        public Builder predictionThreshold(int threshold) {
            this.predictionThreshold = threshold;
            return this;
        }

        public Builder useMemoryPool(boolean use) {
            this.useMemoryPool = use;
            return this;
        }

        public Builder memoryPoolSize(int size) {
            this.memoryPoolSize = size;
            return this;
        }

        public Builder compressionEnabled(boolean enable) {
            this.compressionEnabled = enable;
            return this;
        }

        public Builder compressionThreshold(int threshold) {
            this.compressionThreshold = threshold;
            return this;
        }

        public Builder compressionLevel(int level) {
            this.compressionLevel = level;
            return this;
        }

        public Builder multiLevelEnabled(boolean enable) {
            this.multiLevelEnabled = enable;
            return this;
        }

        public Builder partitionEnabled(boolean enable) {
            this.partitionEnabled = enable;
            return this;
        }

        public Builder partitionCount(int count) {
            this.partitionCount = count;
            return this;
        }

        public Builder performanceMonitorEnabled(boolean enabled) {
            this.performanceMonitorEnabled = enabled;
            return this;
        }

        public Builder monitoringWindow(int size, TimeUnit unit) {
            this.monitoringWindowSize = size;
            this.monitoringWindowUnit = unit;
            return this;
        }

        public Builder autoTuneEnabled(boolean enabled) {
            this.autoTuneEnabled = enabled;
            return this;
        }

        public Builder hotspotPreloadEnabled(boolean enabled) {
            this.hotspotPreloadEnabled = enabled;
            return this;
        }

        public Builder preloadThreads(int threads) {
            this.preloadThreads = threads;
            return this;
        }

        public Builder preloadThreshold(int threshold) {
            this.preloadThreshold = threshold;
            return this;
        }

        public Builder shardCount(int count) {
            this.shardCount = count;
            return this;
        }

        public Builder loader(Function<K, V> loader) {
            this.loader = loader;
            return this;
        }

        public Builder batchSize(int size) {
            this.batchSize = size;
            return this;
        }

        public Builder batchQueueCapacity(int capacity) {
            this.batchQueueCapacity = capacity;
            return this;
        }

        public Builder batchThreads(int threads) {
            this.batchThreads = threads;
            return this;
        }

        public Builder transactionMaxRetries(int retries) {
            this.transactionMaxRetries = retries;
            return this;
        }

        public Builder transactionTimeout(long timeout, TimeUnit unit) {
            this.transactionTimeout = unit.toMillis(timeout);
            return this;
        }

        public Builder prefetchEnabled(boolean enabled) {
            this.prefetchEnabled = enabled;
            return this;
        }

        public Builder prefetchPatternLength(int length) {
            this.prefetchPatternLength = length;
            return this;
        }

        public Builder prefetchThreads(int threads) {
            this.prefetchThreads = threads;
            return this;
        }

        public Builder prefetchThreshold(double threshold) {
            this.prefetchThreshold = threshold;
            return this;
        }

        public Builder optimizeForWrite() {
            this.writeBufferSize = 16384;
            this.useMemoryPool = true;
            this.useDirectMemory = true;
            return this;
        }

        public Builder optimizeForRead() {
            this.concurrencyLevel = Runtime.getRuntime().availableProcessors() * 2;
            this.useDirectMemory = false;
            return this;
        }

        public CacheConfig<K, V> build() {
            return new CacheConfig<>(this);
        }
    }

    // Getters
    public int getMaxSize() { return maxSize; }
    public long getDefaultTTL() { return defaultTTL; }
    public int getCleanupIntervalSeconds() { return cleanupIntervalSeconds; }
    public EvictionPolicy getEvictionPolicy() { return evictionPolicy; }
    public boolean isMetricsEnabled() { return enableMetrics; }
    public long getMaxMemory() { return maxMemory; }
    public int getConcurrencyLevel() { return concurrencyLevel; }
    public boolean isEnableWarmup() { return enableWarmup; }
    public boolean isAsyncWrite() { return asyncWrite; }
    public int getWriteBufferSize() { return writeBufferSize; }
    public double getBloomFilterFpp() { return bloomFilterFpp; }
    public boolean isEnablePrediction() { return enablePrediction; }
    public int getPredictionThreshold() { return predictionThreshold; }
    public boolean isUseMemoryPool() { return useMemoryPool; }
    public int getMemoryPoolSize() { return memoryPoolSize; }
    public boolean isCompressionEnabled() { return compressionEnabled; }
    public int getCompressionThreshold() { return compressionThreshold; }
    public int getCompressionLevel() { return compressionLevel; }
    public boolean isMultiLevelEnabled() { return multiLevelEnabled; }
    public boolean isPartitionEnabled() { return partitionEnabled; }
    public int getPartitionCount() { return partitionCount; }
    public boolean isPerformanceMonitorEnabled() { return performanceMonitorEnabled; }
    public int getMonitoringWindowSize() { return monitoringWindowSize; }
    public TimeUnit getMonitoringWindowUnit() { return monitoringWindowUnit; }
    public boolean isAutoTuneEnabled() { return autoTuneEnabled; }
    public boolean isHotspotPreloadEnabled() { return hotspotPreloadEnabled; }
    public int getPreloadThreads() { return preloadThreads; }
    public int getPreloadThreshold() { return preloadThreshold; }
    public int getShardCount() { return shardCount; }
    public Function<K, V> getLoader() { return loader; }
    public int getBatchSize() { return batchSize; }
    public int getBatchQueueCapacity() { return batchQueueCapacity; }
    public int getBatchThreads() { return batchThreads; }
    public int getTransactionMaxRetries() { return transactionMaxRetries; }
    public long getTransactionTimeout() { return transactionTimeout; }
    public boolean isPrefetchEnabled() { return prefetchEnabled; }
    public int getPrefetchPatternLength() { return prefetchPatternLength; }
    public int getPrefetchThreads() { return prefetchThreads; }
    public double getPrefetchThreshold() { return prefetchThreshold; }
    public boolean isAdaptiveEnabled() { return adaptiveEnabled; }
    public boolean isSnapshotEnabled() { return snapshotEnabled; }
    public int getIndexSegments() { return indexSegments; }
    public int getIndexBuckets() { return indexBuckets; }
    public long getIndexOptimizationInterval() { return indexOptimizationInterval; }
    public boolean isTieredStorageEnabled() { return tieredStorageEnabled; }
    public long getHeapStorageSize() { return heapStorageSize; }
    public long getDirectStorageSize() { return directStorageSize; }
    public long getDiskStorageSize() { return diskStorageSize; }
    public int getStorageMigrationThreads() { return storageMigrationThreads; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public int getWarmupThreads() { return warmupThreads; }
    public int getWarmupBatchSize() { return warmupBatchSize; }
} 