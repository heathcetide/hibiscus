package hibiscus.cetide.app.component.cache;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.nio.ByteBuffer;

import hibiscus.cetide.app.component.cache.adaptive.AdaptiveStrategy;
import hibiscus.cetide.app.component.cache.analytics.AnalyticsConfig;
import hibiscus.cetide.app.component.cache.analytics.AnalyticsReport;
import hibiscus.cetide.app.component.cache.analytics.CacheAnalyzer;
import hibiscus.cetide.app.component.cache.analytics.OptimizationRecommendation;
import hibiscus.cetide.app.component.cache.async.AsyncWriter;
import hibiscus.cetide.app.component.cache.async.RingBuffer;
import hibiscus.cetide.app.component.cache.batch.BatchProcessor;
import hibiscus.cetide.app.component.cache.cache.CacheEntry;
import hibiscus.cetide.app.component.cache.compression.CompressionManager;
import hibiscus.cetide.app.component.cache.concurrent.StripedLock;
import hibiscus.cetide.app.component.cache.config.CacheConfig;
import hibiscus.cetide.app.component.cache.events.CacheEventNotifier;
import hibiscus.cetide.app.component.cache.eviction.W2TinyLFU;
import hibiscus.cetide.app.component.cache.exception.CacheException;
import hibiscus.cetide.app.component.cache.filter.BloomFilter;
import hibiscus.cetide.app.component.cache.hotspot.HotspotPreloader;
import hibiscus.cetide.app.component.cache.index.CacheIndex;
import hibiscus.cetide.app.component.cache.index.IndexOptimizer;
import hibiscus.cetide.app.component.cache.logging.CacheLogger;
import hibiscus.cetide.app.component.cache.memory.MemoryManager;
import hibiscus.cetide.app.component.cache.memory.MemoryPool;
import hibiscus.cetide.app.component.cache.monitor.CacheMonitor;
import hibiscus.cetide.app.component.cache.monitor.MetricsCollector;
import hibiscus.cetide.app.component.cache.monitor.PerformanceMonitor;
import hibiscus.cetide.app.component.cache.multilevel.MultiLevelCache;
import hibiscus.cetide.app.component.cache.partition.CachePartitionManager;
import hibiscus.cetide.app.component.cache.pool.ObjectPoolManager;
import hibiscus.cetide.app.component.cache.predictor.AccessPredictor;
import hibiscus.cetide.app.component.cache.prefetch.SmartPrefetcher;
import hibiscus.cetide.app.component.cache.router.ShardRouter;
import hibiscus.cetide.app.component.cache.serialization.FastSerializer;
import hibiscus.cetide.app.component.cache.snapshot.CacheSnapshot;
import hibiscus.cetide.app.component.cache.stats.CacheStats;
import hibiscus.cetide.app.component.cache.storage.AdaptiveStorage;
import hibiscus.cetide.app.component.cache.storage.TieredStorage;
import hibiscus.cetide.app.component.cache.transaction.TransactionManager;
import hibiscus.cetide.app.component.cache.utils.SerializationUtils;
import hibiscus.cetide.app.component.cache.warmup.CacheWarmer;
import hibiscus.cetide.app.component.cache.warmup.SmartCacheWarmer;
import hibiscus.cetide.app.component.cache.warmup.WarmupAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HibiscusCache<K, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HibiscusCache.class.getName());
    private final AdaptiveStorage<K, CacheEntry<V>> storage;
    private final ScheduledExecutorService cleanupService;
    private long defaultTTL;
    private int maxSize;
    private final MetricsCollector metrics;
    private CacheConfig<K, V> config;
    private final CacheStats stats;
    private final Map<K, Long> accessTimes; // 用于LRU
    private final Map<K, Long> accessCounts; // 用于LFU
    private StripedLock stripedLock;
    private final MemoryManager memoryManager;
    private final CacheWarmer<K, V> cacheWarmer;
    private int concurrencyLevel;
    private final BloomFilter<K> bloomFilter;
    private final AsyncWriter<K, V> asyncWriter;
    private final RingBuffer<Map.Entry<K, V>> writeBuffer;
    private final MemoryPool memoryPool;
    private final FastSerializer serializer;
    private final AccessPredictor<K> predictor;
    private final CompressionManager compressionManager;
    private final MultiLevelCache<K, V> multiLevelCache;
    private final CachePartitionManager<K, V> partitionManager;
    private final AdaptiveStrategy adaptiveStrategy;
    private final CacheSnapshot<K, V> snapshot;
    private CacheIndex<K> index;
    private final W2TinyLFU<K> evictionPolicy;
    private final PerformanceMonitor perfMonitor;
    private final HotspotPreloader<K, V> hotspotPreloader;
    private final ShardRouter<K> shardRouter;
    private final BatchProcessor<K, V> batchProcessor;
    private final TransactionManager<K, V> transactionManager;
    private final IndexOptimizer<K> indexOptimizer;
    private final SmartPrefetcher<K, V> prefetcher;
    private final TieredStorage<K, V> tieredStorage;
    private final ObjectPoolManager<Object> objectPool;
    private final SmartCacheWarmer<K, V> smartWarmer;
    private final CacheMonitor<K, V> cacheMonitor;
    private final CacheEventNotifier<K, V> eventNotifier;
    private final CacheAnalyzer<K, V> analyzer;
    private long startTime;

    public HibiscusCache(ScheduledExecutorService cleanupService, CacheConfig<K, V> config) {
        this.cleanupService = cleanupService;
        this.config = config;
        this.storage = new AdaptiveStorage<>(new CacheEntrySerializer<>());
        this.stats = new CacheStats();
        this.metrics = config.isMetricsEnabled() ? MetricsCollector.getInstance() : null;

        // 根据驱逐策略初始化相应的数据结构
        switch (config.getEvictionPolicy()) {
            case LRU:
                this.accessTimes = new ConcurrentHashMap<>();
                this.accessCounts = null;
                break;
            case LFU:
                this.accessTimes = null;
                this.accessCounts = new ConcurrentHashMap<>();
                break;
            default:
                this.accessTimes = null;
                this.accessCounts = null;
        }

        startCleanupTask(config.getCleanupIntervalSeconds());
        int processors = Runtime.getRuntime().availableProcessors();
        this.concurrencyLevel = processors * 4;
        this.stripedLock = new StripedLock(concurrencyLevel);
        this.memoryManager = new MemoryManager(
            config.getMaxMemory(),
            unused -> evictIfNeeded()
        );
        this.cacheWarmer = new CacheWarmer<>(
            concurrencyLevel,
            1000
        );

        // 初始化布隆过滤器
        this.bloomFilter = new BloomFilter<>(config.getMaxSize(), 0.01);

        // 初始化异步写入器
        if (config.isAsyncWrite()) {
            this.asyncWriter = new AsyncWriter<>(config.getWriteBufferSize(),
                (key, value) -> {
                    CacheEntry<V> entry = new CacheEntry<>(value, config.getDefaultTTL());
                    storage.put(key, entry);
                });
        } else {
            this.asyncWriter = null;
        }

        // 初始化写缓冲区
        this.writeBuffer = new RingBuffer<>(1024);

        // 初始化内存池
        this.memoryPool = new MemoryPool(new int[]{
            64, 128, 256, 512, 1024, 2048, 4096
        }, 1000);

        // 初始化序列化器
        this.serializer = new FastSerializer();

        // 初始化预测器
        this.predictor = new AccessPredictor<>(10, 0.8);

        // 初始化压缩管理器
        this.compressionManager = new CompressionManager(
            config.getCompressionThreshold(),
            config.getCompressionLevel()
        );

        // 初始化多级缓存
        if (config.isMultiLevelEnabled()) {
            this.multiLevelCache = new MultiLevelCache<>();
            multiLevelCache.addLevel(this, 100, TimeUnit.MICROSECONDS.toNanos(100));
        } else {
            this.multiLevelCache = null;
        }

        // 初始化分区管理器
        if (config.isPartitionEnabled()) {
            this.partitionManager = new CachePartitionManager<>(
                config.getPartitionCount(),
                key -> key.hashCode()
            );
        } else {
            this.partitionManager = null;
        }

        // 初始化自适应策略
        if (config.isAdaptiveEnabled()) {
            this.adaptiveStrategy = new AdaptiveStrategy();
        } else {
            this.adaptiveStrategy = null;
        }

        // 初始化快照机制
        if (config.isSnapshotEnabled()) {
            this.snapshot = new CacheSnapshot<>();
        } else {
            this.snapshot = null;
        }

        // 初始化索引
        initializeIndex();

        // 初始化W-TinyLFU淘汰策略
        this.evictionPolicy = new W2TinyLFU<>(config.getMaxSize());

        // 初始化性能监控
        this.perfMonitor = new PerformanceMonitor(60, TimeUnit.SECONDS);

        // 初始化热点数据预加载器
        if (config.isHotspotPreloadEnabled()) {
            this.hotspotPreloader = new HotspotPreloader<>(
                this,
                config.getLoader(),
                config.getPreloadThreads(),
                config.getPreloadThreshold()
            );
        } else {
            this.hotspotPreloader = null;
        }

        // 初始化分片路由器
        this.shardRouter = new ShardRouter<>(
            config.getShardCount(),
            k -> k.hashCode()
        );

        // 初始化批量处理器
        this.batchProcessor = new BatchProcessor<>(
            this,
            config.getBatchSize(),
            config.getBatchQueueCapacity(),
            config.getBatchThreads()
        );

        // 初始化事务管理器
        this.transactionManager = new TransactionManager<>(
            this,
            config.getTransactionMaxRetries(),
            config.getTransactionTimeout()
        );

        // 初始化索引优化器
        this.indexOptimizer = new IndexOptimizer<>(
            this.index,
            config.getIndexOptimizationInterval()
        );

        // 初始化智能预读取器
        if (config.isPrefetchEnabled()) {
            this.prefetcher = new SmartPrefetcher<>(
                this,
                config.getPrefetchThreads(),
                config.getPrefetchPatternLength(),
                config.getPrefetchThreshold()
            );
        } else {
            this.prefetcher = null;
        }

        // 初始化分层存储
        if (config.isTieredStorageEnabled()) {
            this.tieredStorage = new TieredStorage<>(
                config.getHeapStorageSize(),
                config.getDirectStorageSize(),
                config.getDiskStorageSize(),
                config.getStorageMigrationThreads()
            );
        } else {
            this.tieredStorage = null;
        }

        // 初始化对象池管理器
        this.objectPool = new ObjectPoolManager<>(config.getMaxPoolSize());

        // 注册常用对象的池
        objectPool.registerPool(byte[].class, () -> new byte[1024]);
        objectPool.registerPool(StringBuilder.class, StringBuilder::new);
        objectPool.registerPool(ByteBuffer.class, () -> ByteBuffer.allocate(1024));

        // 初始化智能预热器
        this.smartWarmer = new SmartCacheWarmer<>(
            this,
            config.getWarmupThreads(),
            config.getWarmupBatchSize()
        );

        // 初始化缓存监控器
        this.cacheMonitor = new CacheMonitor<>(storage);

        this.eventNotifier = new CacheEventNotifier<>();

        this.analyzer = new CacheAnalyzer<>(new AnalyticsConfig(config));

        // 添加优化监听器
        analyzer.addOptimizationListener(this::handleOptimization);
        analyzer.addReportListener(this::handleReport);

        CacheLogger.logInfo("CjdisServer initialized with config: " + config);
    }

    public HibiscusCache(Map<K, CacheEntry<V>> cache, ScheduledExecutorService cleanupService, long defaultTTL, int maxSize, MetricsCollector metrics, CacheConfig config, CacheStats stats, Map<K, Long> accessTimes, Map<K, Long> accessCounts, StripedLock stripedLock, MemoryManager memoryManager, CacheWarmer<K, V> cacheWarmer, int concurrencyLevel, BloomFilter<K> bloomFilter, AsyncWriter<K, V> asyncWriter, RingBuffer<Map.Entry<K, V>> writeBuffer, MemoryPool memoryPool, FastSerializer serializer, AccessPredictor<K> predictor, CompressionManager compressionManager, MultiLevelCache<K, V> multiLevelCache, CachePartitionManager<K, V> partitionManager, AdaptiveStrategy adaptiveStrategy, CacheSnapshot<K, V> snapshot, CacheIndex<K> index, W2TinyLFU<K> evictionPolicy, PerformanceMonitor perfMonitor, HotspotPreloader<K, V> hotspotPreloader, ShardRouter<K> shardRouter, BatchProcessor<K, V> batchProcessor, TransactionManager<K, V> transactionManager, IndexOptimizer<K> indexOptimizer, SmartPrefetcher<K, V> prefetcher, TieredStorage<K, V> tieredStorage, ObjectPoolManager<Object> objectPool, SmartCacheWarmer<K, V> smartWarmer, CacheMonitor<K, V> cacheMonitor, CacheEventNotifier<K, V> eventNotifier, CacheAnalyzer<K, V> analyzer) {
        this.storage = new AdaptiveStorage<>(new CacheEntrySerializer<>());
        this.cleanupService = cleanupService;
        this.defaultTTL = defaultTTL;
        this.maxSize = maxSize;
        this.metrics = metrics;
        this.config = config;
        this.stats = stats;
        this.accessTimes = accessTimes;
        this.accessCounts = accessCounts;
        this.stripedLock = stripedLock;
        this.memoryManager = memoryManager;
        this.cacheWarmer = cacheWarmer;
        this.concurrencyLevel = concurrencyLevel;
        this.bloomFilter = bloomFilter;
        this.asyncWriter = asyncWriter;
        this.writeBuffer = writeBuffer;
        this.memoryPool = memoryPool;
        this.serializer = serializer;
        this.predictor = predictor;
        this.compressionManager = compressionManager;
        this.multiLevelCache = multiLevelCache;
        this.partitionManager = partitionManager;
        this.adaptiveStrategy = adaptiveStrategy;
        this.snapshot = snapshot;
        this.index = index;
        this.evictionPolicy = evictionPolicy;
        this.perfMonitor = perfMonitor;
        this.hotspotPreloader = hotspotPreloader;
        this.shardRouter = shardRouter;
        this.batchProcessor = batchProcessor;
        this.transactionManager = transactionManager;
        this.indexOptimizer = indexOptimizer;
        this.prefetcher = prefetcher;
        this.tieredStorage = tieredStorage;
        this.objectPool = objectPool;
        this.smartWarmer = smartWarmer;
        this.cacheMonitor = cacheMonitor;
        this.eventNotifier = eventNotifier;
        this.analyzer = analyzer;
    }

    /**
     * 存储键值对，使用默认TTL
     */
    public void put(K key, V value) {
        if (key == null || value == null) return;
        
        CacheEntry<V> entry = new CacheEntry<>(value, config.getDefaultTTL());
        storage.put(key, entry);
        
        if (metrics != null) {
            metrics.recordOperation("put");
        }
    }

    /**
     * 存储键值对，指定TTL
     */
    public void put(K key, V value, long ttl) throws IOException {
        if (key == null || value == null) {
            return;
        }
        storage.put(key, new CacheEntry<>(value, ttl));
        
        if (metrics != null) {
            metrics.recordOperation("put");
            if (bloomFilter != null) {
                bloomFilter.add(key);
            }
        }
    }

    /**
     * 获取值
     */
    public V get(K key) {
        if (key == null) return null;
        
        CacheEntry<V> entry = storage.get(key);
        if (entry == null) {
            if (stats != null) {
                stats.recordMiss();
            }
            return null;
        }
        
        if (entry.isExpired()) {
            storage.remove(key);
            if (stats != null) {
                stats.recordExpiration();
            }
            return null;
        }
        
        if (stats != null) {
            stats.recordHit();
        }
        
        return entry.getValue();
    }

    /**
     * 获取值，如果不存在则计算
     */
    public V getOrCompute(K key, Function<K, V> computeFunc) throws IOException, CacheException {
        V value = get(key);
        if (value == null) {
            value = computeFunc.apply(key);
            if (value != null) {
                put(key, value);
            }
        }
        return value;
    }

    /**
     * 删除缓存项
     */
    public void remove(K key) {
        storage.remove(key);
        LOGGER.info("Removed key: " + key);
    }

    /**
     * 清空缓存
     */
    public void clear() {
        storage.clear();
        LOGGER.info("Cache cleared");
    }

    /**
     * 获取缓存大小
     */
    public int size() {
        return storage.size();
    }

    /**
     * 检查键是否存在
     */
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    /**
     * 更新或插入值
     */
    public V getAndPut(K key, V value) throws IOException {
        CacheEntry<V> oldEntry = storage.get(key);
        put(key, value);
        return oldEntry != null ? oldEntry.getValue() : null;
    }

    private void startCleanupTask(int intervalSeconds) {
        cleanupService.scheduleAtFixedRate(() -> {
            try {
                cleanup();
            } catch (Exception e) {
                LOGGER.info("Error during cache cleanup", e);
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void cleanup() {
        storage.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void evictOldest() {
        // 使用访问时间而不是创建时间
        if (!accessTimes.isEmpty()) {
            K oldestKey = accessTimes.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            
            if (oldestKey != null) {
                storage.remove(oldestKey);
            }
        } else {
            // 如果没有访问时间记录，随机移除一个
            storage.keySet().stream().findFirst().ifPresent(storage::remove);
        }
    }

    /**
     * 关闭缓存服务
     */
    public void shutdown() {
        if (cleanupService != null) {
            cleanupService.shutdown();
        }
        if (asyncWriter != null) {
            asyncWriter.shutdown();
        }
        storage.clear();
        LOGGER.info("Cache service shutdown completed");
        cacheWarmer.shutdown();
    }

    public Map<String, Object> getMetrics() {
        return metrics.getStats();
    }

    private void updateAccessStats(K key) {
        if (config.isMetricsEnabled()) {
            long currentTime = System.nanoTime();
            CacheConfig.EvictionPolicy policy = config.getEvictionPolicy();
            if (policy == CacheConfig.EvictionPolicy.LRU) {
                accessTimes.put(key, currentTime);
            } else if (policy == CacheConfig.EvictionPolicy.LFU) {
                accessCounts.merge(key, 1L, Long::sum);
            }
        }
    }

    private void evictIfNeeded() {
        while (storage.size() >= config.getMaxSize()) {
            K keyToEvict = selectEvictionCandidate();
            if (keyToEvict != null) {
                storage.remove(keyToEvict);
            } else {
                // 如果没有找到候选项，随机移除一个
                storage.keySet().stream().findFirst().ifPresent(storage::remove);
                break;
            }
        }
    }

    private K selectEvictionCandidate() {
        CacheConfig.EvictionPolicy policy = config.getEvictionPolicy();
        if (policy == CacheConfig.EvictionPolicy.LRU) {
            return accessTimes.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        } else if (policy == CacheConfig.EvictionPolicy.LFU) {
            return accessCounts.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        } else if (policy == CacheConfig.EvictionPolicy.W_TINYLFU) {
            return evictionPolicy.getEvictionCandidate();
        } else if (policy == CacheConfig.EvictionPolicy.FIFO) {
            return storage.keySet().stream().findFirst().orElse(null);
        } else {
            return storage.keySet().stream()
                .skip((int) (storage.size() * Math.random()))
                .findFirst()
                .orElse(null);
        }
    }

    public CacheStats getStats() {
        return stats;
    }

    private long estimateSize(K key, V value) {
        // 简单的内存估算，可以根据实际对象类型优化
        return 16 + // 对象头
               8 + // 引用
               (key instanceof String ? ((String) key).length() * 2L : 8) +
               (value instanceof String ? ((String) value).length() * 2L : 8);
    }

    // 批量操作支持
    public void putAll(Map<K, V> entries) {
        entries.entrySet()
               .parallelStream()
               .forEach(e -> {
                   put(e.getKey(), e.getValue());
               });
    }

    public Map<K, V> getAll(Collection<K> keys) throws IOException, ClassNotFoundException {
        try {
            return keys.parallelStream()
                      .collect(Collectors.toConcurrentMap(
                          k -> k,
                          k -> get(k),
                          (v1, v2) -> v1,
                          ConcurrentHashMap::new
                      ));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            if (e.getCause() instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) e.getCause();
            }
            throw e;
        }
    }

    // 预热支持
    public CompletableFuture<Void> warmup(Collection<K> keys, Function<K, V> loader) {
        return cacheWarmer.warmup(keys, k -> {
            V value = loader.apply(k);
            if (value != null) {
                put(k, value);
            }
            return value;
        });
    }

    // 添加批量预热方法
    public void warmupBatch(Collection<K> keys, Function<K, V> loader, int batchSize) {
        List<List<K>> partitions = new ArrayList<>();
        List<K> currentBatch = new ArrayList<>(batchSize);
        for (K key : keys) {
            currentBatch.add(key);
            if (currentBatch.size() == batchSize) {
                partitions.add(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }
        }
        if (!currentBatch.isEmpty()) {
            partitions.add(currentBatch);
        }
        
        partitions.parallelStream()
            .forEach(batch -> {
                Map<K, V> values = batch.stream()
                    .collect(Collectors.toMap(
                        k -> k,
                        loader::apply
                    ));
                putAll(values);
            });
    }

    /**
     * 直接写入方法，不经过异步处理
     */
    private void directPut(K key, V value) {
        if (value == null) {
            storage.remove(key);
            return;
        }
        
        CacheEntry<V> entry = new CacheEntry<>(value, config.getDefaultTTL());
        storage.put(key, entry);
        
        // 更新相关统计信息
        if (metrics != null) {
            metrics.recordOperation("put");
        }
        
        // 更新布隆过滤器
        if (bloomFilter != null) {
            bloomFilter.add(key);
        }
    }

    private byte[] serializeValue(V value) {
        byte[] buffer = objectPool.acquire(byte[].class);
        try {
            long size = serializer.objectToBytes(value, buffer);
            return Arrays.copyOf(buffer, (int)size);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            objectPool.release(buffer);
        }
    }

    private K getLastAccessedKey() {
        CacheConfig.EvictionPolicy policy = config.getEvictionPolicy();
        if (policy == CacheConfig.EvictionPolicy.LRU) {
            return accessTimes.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        } else if (policy == CacheConfig.EvictionPolicy.LFU) {
            return accessCounts.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        return null;
    }

    private byte[] compressIfNeeded(byte[] data) {
        if (config.isCompressionEnabled()) {
            return compressionManager.compress(data);
        }
        return data;
    }

    private byte[] decompressIfNeeded(byte[] data) {
        if (config.isCompressionEnabled()) {
            return compressionManager.decompress(data);
        }
        return data;
    }

    // 定期检查和应用自适应策略
    private void applyAdaptiveStrategy() {
        if (adaptiveStrategy != null) {
            int optimalSize = adaptiveStrategy.getOptimalSize();
            if (optimalSize != config.getMaxSize()) {
                // 调整缓存大小
                config = new CacheConfig.Builder()
                    .maxSize(optimalSize)
                    .build();
            }
        }
    }

    // 添加性能分析方法
    public void analyzePerformance() {
        double hitRate = stats.getHitRate();
        double avgLatency = perfMonitor.getAverageLatency("get");
        double throughput = perfMonitor.getThroughput("get");
        
        // 根据性能指标进行优化
        if (hitRate < 0.8 && throughput > 1000) {
            updateConfig(new CacheConfig.Builder<K, V>()
                .maxSize((int)(config.getMaxSize() * 1.5))
                .build());
        }
        
        if (avgLatency > 1_000_000) {
            updateConfig(new CacheConfig.Builder<K, V>()
                .concurrencyLevel(config.getConcurrencyLevel() * 2)
                .build());
        }
    }

    // 添加批量操作方法
    public CompletableFuture<Map<K, V>> putAllAsync(Map<K, V> entries) {
        return batchProcessor.submitBatch(entries, BatchProcessor.OperationType.PUT);
    }

    public CompletableFuture<Map<K, V>> getAllAsync(Set<K> keys) {
        return batchProcessor.submitBatch(
            keys.stream().collect(Collectors.toMap(k -> k, k -> null)),
            BatchProcessor.OperationType.GET
        );
    }

    public CompletableFuture<Void> removeAllAsync(Set<K> keys) {
        return batchProcessor.submitBatch(
            keys.stream().collect(Collectors.toMap(k -> k, k -> null)),
            BatchProcessor.OperationType.REMOVE
        ).thenAccept(ignored -> {});
    }

    // 添加事务相关方法
    public long beginTransaction() {
        return transactionManager.begin();
    }

    public V getWithTransaction(long txId, K key) throws TransactionManager.TransactionException, IOException, ClassNotFoundException, CacheException {
        return transactionManager.get(txId, key);
    }

    public void putWithTransaction(long txId, K key, V value) throws TransactionManager.TransactionException {
        transactionManager.put(txId, key, value);
    }

    public boolean commitTransaction(long txId) throws TransactionManager.TransactionException {
        return transactionManager.commit(txId);
    }

    public void rollbackTransaction(long txId) {
        transactionManager.rollback(txId);
    }

    // 添加事务执行助手方法
    public <R> R executeInTransaction(TransactionCallback<K, V, R> callback) {
        int retries = 0;
        while (retries < config.getTransactionMaxRetries()) {
            long txId = beginTransaction();
            try {
                R result = callback.execute(txId, this);
                if (commitTransaction(txId)) {
                    return result;
                }
            } catch (TransactionManager.TransactionException e) {
                rollbackTransaction(txId);
            }
            retries++;
        }
        throw new RuntimeException("Transaction failed after " + retries + " retries");
    }


    @FunctionalInterface
    public interface TransactionCallback<K, V, R> {
        R execute(long txId, HibiscusCache<K, V> cache) throws TransactionManager.TransactionException;
    }

    // 添加智能预热方法
    public void smartWarmup(Function<K, V> loader) {
        smartWarmer.warmup(loader);
    }

    // 添加预热分析方法
    public WarmupAnalysis getWarmupAnalysis() {
        return smartWarmer.getAnalysis();
    }

    // 添加事件监听器注册方法
    public void addCacheEventListener(Consumer<CacheEventNotifier.CacheEvent<K, V>> listener) {
        eventNotifier.addListener(listener);
    }

    public void removeCacheEventListener(Consumer<CacheEventNotifier.CacheEvent<K, V>> listener) {
        eventNotifier.removeListener(listener);
    }

    private void handleOptimization(OptimizationRecommendation recommendation) {
        switch (recommendation.getType()) {
            case INCREASE_CACHE_SIZE:
                double factor = (Double) recommendation.getParameters().get("increase_factor");
                int newSize = (int) (config.getMaxSize() * factor);
                resizeCache(newSize);
                break;
            case ADJUST_TTL:
                double ttlFactor = (Double) recommendation.getParameters().get("ttl_factor");
                adjustTTL(ttlFactor);
                break;
            case PRELOAD_KEYS:
                Set<K> keys = (Set<K>) recommendation.getParameters().get("keys");
                preloadKeys(keys);
                break;
        }
    }

    private void handleReport(AnalyticsReport report) {
        CacheLogger.logInfo("Analytics Report: " + report.toString());
    }

    private void resizeCache(int newSize) {
        // 实现缓存大小调整逻辑
    }

    private void adjustTTL(double factor) {
        // 实现TTL调整逻辑
    }

    private void preloadKeys(Set<K> keys) {
        // 实现键预加载逻辑
    }

    private void updateConfig(CacheConfig<K, V> newConfig) {
        this.maxSize = newConfig.getMaxSize();
        this.defaultTTL = newConfig.getDefaultTTL();
        
        if (newConfig.getConcurrencyLevel() != config.getConcurrencyLevel()) {
            int newLevel = Math.toIntExact(newConfig.getConcurrencyLevel());
            this.stripedLock = new StripedLock(newLevel);
        }
        
        this.config = newConfig;
    }

    public void recordAccess(K key) {
        if (prefetcher != null) {
            prefetcher.recordKeyAccess(key);
        }
    }

    private List<K> getKeysToEvict() {
        if (evictionPolicy != null) {
            K candidate = evictionPolicy.getEvictionCandidate();
            return candidate != null ? Collections.singletonList(candidate) : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private void compressAndStore(K key, V value) throws IOException {
        byte[] data = serializer.serialize(value);
        // ... 其他代码
    }

    private void initializeComponents() {
        int processors = Runtime.getRuntime().availableProcessors();
        this.concurrencyLevel = processors * 4;
        this.stripedLock = new StripedLock(concurrencyLevel);
    }

    private void initializeIndex() {
        long intervalLong = config.getIndexOptimizationInterval();
        if (intervalLong > Integer.MAX_VALUE || intervalLong < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Index optimization interval is out of int range");
        }
        int interval = (int) intervalLong;
        
        int segments = config.getIndexSegments();
        int buckets = config.getIndexBuckets();
        
        this.index = new CacheIndex<>(segments, buckets, k -> (long) k.hashCode());
    }

    private static class CacheEntrySerializer<V> implements AdaptiveStorage.Serializer<CacheEntry<V>> {
        @Override
        public byte[] serialize(CacheEntry<V> entry) {
            // 实现序列化逻辑
            return SerializationUtils.serialize(entry);
        }
        
        @Override
        public CacheEntry<V> deserialize(byte[] data) {
            // 实现反序列化逻辑
            return SerializationUtils.deserialize(data);
        }
    }

    public AdaptiveStorage<K, CacheEntry<V>> getStorage() {
        return storage;
    }

    public ScheduledExecutorService getCleanupService() {
        return cleanupService;
    }

    public long getDefaultTTL() {
        return defaultTTL;
    }

    public void setDefaultTTL(long defaultTTL) {
        this.defaultTTL = defaultTTL;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public CacheConfig<K, V> getConfig() {
        return config;
    }

    public void setConfig(CacheConfig<K, V> config) {
        this.config = config;
    }

    public Map<K, Long> getAccessTimes() {
        return accessTimes;
    }

    public Map<K, Long> getAccessCounts() {
        return accessCounts;
    }

    public StripedLock getStripedLock() {
        return stripedLock;
    }

    public void setStripedLock(StripedLock stripedLock) {
        this.stripedLock = stripedLock;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public CacheWarmer<K, V> getCacheWarmer() {
        return cacheWarmer;
    }

    public int getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public void setConcurrencyLevel(int concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }

    public BloomFilter<K> getBloomFilter() {
        return bloomFilter;
    }

    public AsyncWriter<K, V> getAsyncWriter() {
        return asyncWriter;
    }

    public RingBuffer<Map.Entry<K, V>> getWriteBuffer() {
        return writeBuffer;
    }

    public MemoryPool getMemoryPool() {
        return memoryPool;
    }

    public FastSerializer getSerializer() {
        return serializer;
    }

    public AccessPredictor<K> getPredictor() {
        return predictor;
    }

    public CompressionManager getCompressionManager() {
        return compressionManager;
    }

    public MultiLevelCache<K, V> getMultiLevelCache() {
        return multiLevelCache;
    }

    public CachePartitionManager<K, V> getPartitionManager() {
        return partitionManager;
    }

    public AdaptiveStrategy getAdaptiveStrategy() {
        return adaptiveStrategy;
    }

    public CacheSnapshot<K, V> getSnapshot() {
        return snapshot;
    }

    public CacheIndex<K> getIndex() {
        return index;
    }

    public void setIndex(CacheIndex<K> index) {
        this.index = index;
    }

    public W2TinyLFU<K> getEvictionPolicy() {
        return evictionPolicy;
    }

    public PerformanceMonitor getPerfMonitor() {
        return perfMonitor;
    }

    public HotspotPreloader<K, V> getHotspotPreloader() {
        return hotspotPreloader;
    }

    public ShardRouter<K> getShardRouter() {
        return shardRouter;
    }

    public BatchProcessor<K, V> getBatchProcessor() {
        return batchProcessor;
    }

    public TransactionManager<K, V> getTransactionManager() {
        return transactionManager;
    }

    public IndexOptimizer<K> getIndexOptimizer() {
        return indexOptimizer;
    }

    public SmartPrefetcher<K, V> getPrefetcher() {
        return prefetcher;
    }

    public TieredStorage<K, V> getTieredStorage() {
        return tieredStorage;
    }

    public ObjectPoolManager<Object> getObjectPool() {
        return objectPool;
    }

    public SmartCacheWarmer<K, V> getSmartWarmer() {
        return smartWarmer;
    }

    public CacheMonitor<K, V> getCacheMonitor() {
        return cacheMonitor;
    }

    public CacheEventNotifier<K, V> getEventNotifier() {
        return eventNotifier;
    }

    public CacheAnalyzer<K, V> getAnalyzer() {
        return analyzer;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * 获取缓存中的所有键
     * @return 键的集合
     */
    public Set<K> getKeys() {
        return storage.keySet();
    }
    
    /**
     * 获取缓存中的所有键值对
     * @return 键值对的集合
     */
    public Map<K, V> getAll() {
        Map<K, V> result = new HashMap<>();
        storage.keySet().forEach(key -> {
            CacheEntry<V> entry = storage.get(key);
            if (entry != null && !entry.isExpired()) {
                result.put(key, entry.getValue());
            }
        });
        return result;
    }

    /**
     * 获取当前缓存的内存占用估算
     * @return 内存占用（字节）
     */
    public long getMemoryUsage() {
        long totalSize = 0;
        Map<K, V> snapshot = getAll(); // 获取当前所有缓存项

        for (Map.Entry<K, V> entry : snapshot.entrySet()) {
            // 计算key的大小
            totalSize += getObjectSize(entry.getKey());
            // 计算value的大小
            totalSize += getObjectSize(entry.getValue());
        }

        return totalSize;
    }

    /**
     * 估算对象大小
     */
    private long getObjectSize(Object obj) {
        if (obj == null) return 0;

        if (obj instanceof String) {
            return 40 + ((String) obj).length() * 2L; // 字符串额外开销 + 每个字符2字节
        }

        if (obj instanceof Number) {
            if (obj instanceof Long || obj instanceof Double) {
                return 24; // Long/Double 类型占用
            }
            return 16; // Integer/Short/Byte 等类型占用
        }

        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            long size = 40; // Map基础开销
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                size += getObjectSize(entry.getKey());
                size += getObjectSize(entry.getValue());
            }
            return size;
        }

        if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            long size = 40; // Collection基础开销
            for (Object item : collection) {
                size += getObjectSize(item);
            }
            return size;
        }
        return 40; // 默认对象头大小
    }
}