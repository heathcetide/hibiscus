package hibiscus.cetide.app.component.cache.warmup;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class CacheWarmer<K, V> {
    private final ExecutorService executor;
    private final int batchSize;

    public CacheWarmer(int threadCount, int batchSize) {
        this.executor = Executors.newFixedThreadPool(threadCount);
        this.batchSize = batchSize;
    }

    public CompletableFuture<Void> warmup(Collection<K> keys, Function<K, V> loader) {
        return CompletableFuture.runAsync(() -> {
            keys.stream()
                .parallel()
                .forEach(loader::apply);
        }, executor);
    }

    public void shutdown() {
        executor.shutdown();
    }
} 