package hibiscus.cetide.app.component.cache.storage;


import hibiscus.cetide.app.component.cache.serialization.FastSerializer;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.Map;
import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.zip.Deflater;
import java.nio.file.Paths;

public class TieredStorage<K, V> {
    private final ConcurrentHashMap<K, StorageLocation> locationMap;
    private final MemoryTier memoryTier;
    private final DirectMemoryTier directMemoryTier;
    private final DiskTier diskTier;
    private final ExecutorService migrationExecutor;
    private final FastSerializer serializer;
    private final CompressedStorage<K, byte[]> compressedStorage;
    
    public enum StorageType {
        HEAP, DIRECT, DISK
    }
    
    private static class StorageLocation {
        final StorageType type;
        final long offset;
        final int size;
        volatile long lastAccess;
        
        public StorageLocation(StorageType type, long offset, int size) {
            this.type = type;
            this.offset = offset;
            this.size = size;
            this.lastAccess = System.nanoTime();
        }
    }
    
    public TieredStorage(long heapSize, long directSize, long diskSize, int threads) {
        this.locationMap = new ConcurrentHashMap<>();
        this.memoryTier = new MemoryTier<>(heapSize);
        this.directMemoryTier = new DirectMemoryTier<>(directSize);
        this.diskTier = new DiskTier<>(Paths.get("disk_storage"), diskSize);
        this.migrationExecutor = Executors.newFixedThreadPool(threads);
        this.serializer = new FastSerializer();
        
        this.compressedStorage = new CompressedStorage<>(
            "your-encryption-key-here",
            Deflater.BEST_COMPRESSION,
            Runtime.getRuntime().availableProcessors()
        );
        
        startMigrationTask();
    }
    
    private void startMigrationTask() {
        migrationExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    migrateData();
                    Thread.sleep(1000); // 控制迁移频率
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    private void migrateData() {
        if (directMemoryTier.hasSpace()) {
            Map<K, StorageLocation> hotEntries = diskTier.getHotEntries();
            hotEntries.forEach((key, value) -> {
                try {
                    byte[] data = diskTier.read(value.offset, value.size);
                    long offset = directMemoryTier.write(data);
                    StorageLocation newLocation = new StorageLocation(StorageType.DIRECT, offset, data.length);
                    locationMap.put(key, newLocation);
                    diskTier.getHotEntries().remove(key);
                } catch (Exception e) {
                    // 记录迁移错误但继续处理
                }
            });
        }
    }
    
    public void put(K key, V value) throws IOException {
        byte[] data = serializer.serialize(value);
        
        // 对数据进行压缩和加密存储
        compressedStorage.put(key, data);
        
        StorageLocation location;
        
        // 尝试按优先级存储
        if (memoryTier.hasSpace()) {
            long offset = memoryTier.write(data);
            location = new StorageLocation(StorageType.HEAP, offset, data.length);
        } else if (directMemoryTier.hasSpace()) {
            long offset = directMemoryTier.write(data);
            location = new StorageLocation(StorageType.DIRECT, offset, data.length);
        } else {
            long offset = diskTier.write(data);
            location = new StorageLocation(StorageType.DISK, offset, data.length);
        }
        
        locationMap.put(key, location);
    }
    
    @SuppressWarnings("unchecked")
    public V get(K key) throws IOException, ClassNotFoundException {
        // 首先尝试从压缩存储中获取
        byte[] compressedData = compressedStorage.get(key);
        if (compressedData != null) {
            return serializer.deserialize(compressedData);
        }
        
        StorageLocation location = locationMap.get(key);
        if (location == null) return null;
        
        location.lastAccess = System.nanoTime();
        byte[] data;
        
        switch (location.type) {
            case HEAP:
                data = memoryTier.read(location.offset, location.size);
                break;
            case DIRECT:
                ByteBuffer buffer = directMemoryTier.read(location.offset, location.size);
                data = new byte[location.size];
                buffer.get(data);
                break;
            case DISK:
                data = diskTier.read(location.offset, location.size);
                break;
            default:
                throw new IllegalStateException("Unknown storage type");
        }
        
        return (V) serializer.deserialize(data);
    }
    
    public void shutdown() {
        migrationExecutor.shutdown();
        memoryTier.close();
        directMemoryTier.close();
        diskTier.close();
    }
} 