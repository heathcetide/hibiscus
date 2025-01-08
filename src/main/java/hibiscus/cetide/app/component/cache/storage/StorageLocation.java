package hibiscus.cetide.app.component.cache.storage;

public class StorageLocation {
    public final long offset;
    public final int size;
    public volatile long lastAccess;

    public StorageLocation(long offset, int size) {
        this.offset = offset;
        this.size = size;
        this.lastAccess = System.nanoTime();
    }
} 