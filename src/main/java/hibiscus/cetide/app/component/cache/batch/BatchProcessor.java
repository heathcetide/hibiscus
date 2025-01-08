package hibiscus.cetide.app.component.cache.batch;


import hibiscus.cetide.app.component.cache.HibiscusCache;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BatchProcessor<K, V> {
    private static final int BATCH_TIMEOUT_MS = 100;
    private final int batchSize;
    private final int queueCapacity;
    private final ExecutorService executor;
    private final ArrayBlockingQueue<BatchOperation<K, V>> operationQueue;
    private final HibiscusCache<K, V> cache;
    
    public static class BatchOperation<K, V> {
        final Map<K, V> entries;
        final CompletableFuture<Map<K, V>> future;
        final OperationType type;
        
        public BatchOperation(Map<K, V> entries, OperationType type) {
            this.entries = entries;
            this.future = new CompletableFuture<>();
            this.type = type;
        }
    }
    
    public enum OperationType {
        PUT, GET, REMOVE
    }
    
    public BatchProcessor(HibiscusCache<K, V> cache, int batchSize, int queueCapacity, int threads) {
        this.cache = cache;
        this.batchSize = batchSize;
        this.queueCapacity = queueCapacity;
        this.executor = Executors.newFixedThreadPool(threads);
        this.operationQueue = new ArrayBlockingQueue<>(queueCapacity);
        
        startProcessing();
    }
    
    private void startProcessing() {
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<BatchOperation<K, V>> batch = new ArrayList<>();
                    BatchOperation<K, V> operation = operationQueue.take();
                    batch.add(operation);
                    
                    // 尝试收集更多操作形成批量
                    operationQueue.drainTo(batch, batchSize - 1);
                    
                    // 按类型分组处理
                    Map<OperationType, List<BatchOperation<K, V>>> groupedOps = 
                        batch.stream().collect(Collectors.groupingBy(op -> op.type));
                    // 处理每种类型的操作
                    for (Map.Entry<OperationType, List<BatchOperation<K, V>>> entry: groupedOps.entrySet()) {
                        processBatchByType(entry.getKey(), entry.getValue());
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    private void processBatchByType(OperationType type, List<BatchOperation<K, V>> operations) {
        // 使用 CompletableFuture 进行并行处理
        CompletableFuture.runAsync(() -> {
            switch (type) {
                case PUT:
                    Map<K, V> allEntries = operations.stream()
                        .flatMap(op -> op.entries.entrySet().stream())
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (v1, v2) -> v2
                        ));
                    cache.putAll(allEntries);
                    operations.forEach(op -> op.future.complete(op.entries));
                    break;
            }
        }, executor);
    }
    
    public CompletableFuture<Map<K, V>> submitBatch(Map<K, V> entries, OperationType type) {
        BatchOperation<K, V> operation = new BatchOperation<>(entries, type);
        try {
            operationQueue.put(operation);
        } catch (InterruptedException e) {
            operation.future.completeExceptionally(e);
            Thread.currentThread().interrupt();
        }
        return operation.future;
    }
    
    public void shutdown() {
        executor.shutdown();
    }
} 