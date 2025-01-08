package hibiscus.cetide.app.component.cache.memory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class MemoryManager {
    private final long maxMemory;
    private final AtomicLong usedMemory;
    private final Consumer<Void> evictionCallback;
    private static final int ALLOCATION_RETRIES = 3;

    public MemoryManager(long maxMemory, Consumer<Void> evictionCallback) {
        this.maxMemory = maxMemory;
        this.usedMemory = new AtomicLong(0);
        this.evictionCallback = evictionCallback;
    }

    public boolean allocate(long size) {
        for (int i = 0; i < ALLOCATION_RETRIES; i++) {
            long current = usedMemory.get();
            long next = current + size;
            if (next > maxMemory) {
                evictionCallback.accept(null);
                continue;
            }
            if (usedMemory.compareAndSet(current, next)) {
                return true;
            }
        }
        return false;
    }

    public void free(long size) {
        usedMemory.addAndGet(-size);
    }

    public double getMemoryUsageRatio() {
        return (double) usedMemory.get() / maxMemory;
    }
} 