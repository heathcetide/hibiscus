package hibiscus.cetide.app.component.cache.async;

import hibiscus.cetide.app.component.cache.logging.CacheLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;

import java.util.ArrayDeque;

public class AsyncWriter<K, V> {
    private final RingBuffer<CacheOperation<K, V>> operationBuffer;
    private final ExecutorService executor;
    private final BiConsumer<K, V> writeHandler;
    private volatile boolean running = true;
    
    private static final int SPIN_TRIES = 1000;
    private static final int BATCH_SIZE = 64;
    private final ThreadLocal<ArrayDeque<CacheOperation<K, V>>> localBatch = 
        ThreadLocal.withInitial(() -> new ArrayDeque<>(BATCH_SIZE));

    public static class CacheOperation<K, V> {
        final K key;
        final V value;
        final OperationType type;

        public CacheOperation(K key, V value, OperationType type) {
            this.key = key;
            this.value = value;
            this.type = type;
        }
    }

    public enum OperationType {
        PUT, REMOVE
    }

    public AsyncWriter(int bufferSize, BiConsumer<K, V> writeHandler) {
        this.operationBuffer = new RingBuffer<>(bufferSize);
        this.writeHandler = writeHandler;
        this.executor = Executors.newSingleThreadExecutor();
        startProcessing();
    }

    private void startProcessing() {
        executor.submit(() -> {
            ArrayDeque<CacheOperation<K, V>> batch = localBatch.get();
            while (running) {
                try {
                    // 批量处理操作
                    CacheOperation<K, V> op = operationBuffer.poll();
                    if (op != null) {
                        batch.offer(op);
                        
                        // 尝试收集更多操作
                        for (int i = 0; i < BATCH_SIZE - 1 && !batch.isEmpty(); i++) {
                            CacheOperation<K, V> nextOp = operationBuffer.poll();
                            if (nextOp == null) break;
                            batch.offer(nextOp);
                        }
                        
                        // 批量处理
                        for (CacheOperation<K, V> operation : batch) {
                            processOperation(operation);
                        }
                        batch.clear();
                    } else {
                        LockSupport.parkNanos(100);
                    }
                } catch (Exception e) {
                    CacheLogger.logError("Error processing async operation", e);
                }
            }
        });
    }

    private void processOperation(CacheOperation<K, V> operation) {
        try {
            System.out.println("Processing operation: key=" + operation.key + ", value=" + operation.value);
            writeHandler.accept(operation.key, operation.value);
        } catch (Exception e) {
            CacheLogger.logError("Error processing operation", e);
        }
    }

    public void asyncPut(K key, V value) {
        CacheOperation<K, V> op = new CacheOperation<>(key, value, OperationType.PUT);
        int tries = SPIN_TRIES;
        
        while (tries-- > 0) { // 先尝试自旋
            if (operationBuffer.offer(op)) {
                return;
            }
        }
        
        // 自旋失败后再使用 park
        while (!operationBuffer.offer(op)) {
            LockSupport.parkNanos(1000);
        }
    }

    public void asyncRemove(K key) {
        while (!operationBuffer.offer(new CacheOperation<>(key, null, OperationType.REMOVE))) {
            LockSupport.parkNanos(1000);
        }
    }

    public void shutdown() {
        running = false;
        executor.shutdown();
    }

    public void waitForCompletion() {
        // 等待所有操作完成
        while (!operationBuffer.isEmpty()) {
            Thread.yield();
        }
        
        // 确保处理线程有时间处理完所有操作
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
} 