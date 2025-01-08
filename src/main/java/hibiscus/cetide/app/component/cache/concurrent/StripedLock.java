package hibiscus.cetide.app.component.cache.concurrent;

import java.util.concurrent.locks.ReentrantLock;

public class StripedLock {
    private static final int DEFAULT_CONCURRENCY = Runtime.getRuntime().availableProcessors() * 4;
    private final ReentrantLock[] locks;
    private final int mask;

    public StripedLock() {
        this(DEFAULT_CONCURRENCY);
    }

    public StripedLock(int concurrencyLevel) {
        int stripes = Math.max(1, Integer.highestOneBit(concurrencyLevel));
        locks = new ReentrantLock[stripes];
        mask = stripes - 1;
        for (int i = 0; i < stripes; i++) {
            locks[i] = new ReentrantLock(false);
        }
    }

    public ReentrantLock getLock(Object key) {
        return locks[spread(key.hashCode()) & mask];
    }

    private static int spread(int h) {
        h ^= (h >>> 16) ^ (h >>> 32);
        return h ^ (h >>> 16);
    }

    public void lockAll() {
        for (ReentrantLock lock : locks) {
            lock.lock();
        }
    }

    public void unlockAll() {
        for (ReentrantLock lock : locks) {
            lock.unlock();
        }
    }
} 