package hibiscus.cetide.app.component.cache.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.*;
import java.util.zip.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.Map;

public class CompressedStorage<K, V> {
    private final ConcurrentHashMap<K, CompressedEntry> entries;
    private final ExecutorService compressionExecutor;
    private final Cipher cipher;
    private final int compressionLevel;
    private final StorageMetrics metrics;
    private final String encryptionKey;
    
    private static class CompressedEntry {
        final byte[] data;
        final int originalSize;
        final long timestamp;
        final CompressionType type;
        
        public CompressedEntry(byte[] data, int originalSize, CompressionType type) {
            this.data = data;
            this.originalSize = originalSize;
            this.timestamp = System.nanoTime();
            this.type = type;
        }
    }
    
    public enum CompressionType {
        NONE, GZIP, LZ4, ENCRYPTED
    }
    
    public CompressedStorage(String encryptionKey, int compressionLevel, int threads) {
        this.encryptionKey = encryptionKey;
        this.entries = new ConcurrentHashMap<>();
        this.compressionExecutor = Executors.newFixedThreadPool(threads);
        this.compressionLevel = compressionLevel;
        this.metrics = new StorageMetrics();
        
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                encryptionKey.getBytes(), "AES");
            this.cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption", e);
        }
    }
    
    public void put(K key, byte[] value) {
        long startTime = System.nanoTime();
        CompletableFuture.supplyAsync(() -> {
            try {
                // 选择最佳压缩方式
                CompressionType type = selectCompressionType(value);
                byte[] compressed = compress(value, type);
                
                // 如果压缩效果不好，使用原始数据
                if (compressed.length >= value.length && type != CompressionType.ENCRYPTED) {
                    compressed = value;
                    type = CompressionType.NONE;
                }
                
                return new CompressedEntry(compressed, value.length, type);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, compressionExecutor).thenAccept(entry -> {
            entries.put(key, entry);
            metrics.recordCompression(
                entry.originalSize, 
                entry.data.length, 
                System.nanoTime() - startTime
            );
        });
    }
    
    public byte[] get(K key) {
        CompressedEntry entry = entries.get(key);
        if (entry == null) return null;
        
        long startTime = System.nanoTime();
        try {
            byte[] decompressed = decompress(entry.data, entry.type);
            metrics.recordDecompression(
                entry.data.length,
                decompressed.length,
                System.nanoTime() - startTime
            );
            return decompressed;
        } catch (Exception e) {
            metrics.recordError();
            throw new RuntimeException("Decompression failed", e);
        }
    }
    
    private CompressionType selectCompressionType(byte[] data) {
        // 基于数据特征选择压缩方式
        if (needsEncryption(data)) {
            return CompressionType.ENCRYPTED;
        }
        if (data.length > 1024 && isCompressible(data)) {
            return CompressionType.GZIP;
        }
        if (data.length <= 1024 && isCompressible(data)) {
            return CompressionType.LZ4;
        }
        return CompressionType.NONE;
    }
    
    private boolean needsEncryption(byte[] data) {
        // 检查数据是否需要加密
        return containsSensitiveData(data);
    }
    
    private boolean isCompressible(byte[] data) {
        // 通过采样检查数据是否可压缩
        int uniqueBytes = 0;
        boolean[] seen = new boolean[256];
        for (int i = 0; i < Math.min(1000, data.length); i++) {
            if (!seen[data[i] & 0xFF]) {
                seen[data[i] & 0xFF] = true;
                uniqueBytes++;
            }
        }
        return uniqueBytes < 200; // 如果唯一字节数较少，说明数据可能比较容易压缩
    }
    
    private byte[] compress(byte[] data, CompressionType type) throws Exception {
        switch (type) {
            case GZIP:
                return compressGzip(data);
            case LZ4:
                return compressLz4(data);
            case ENCRYPTED:
                return encrypt(data);
            default:
                return data;
        }
    }
    
    private byte[] decompress(byte[] data, CompressionType type) throws Exception {
        switch (type) {
            case GZIP:
                return decompressGzip(data);
            case LZ4:
                return decompressLz4(data);
            case ENCRYPTED:
                return decrypt(data);
            default:
                return data;
        }
    }
    
    private byte[] compressGzip(byte[] data) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOS = new GZIPOutputStream(bos, compressionLevel)) {
            gzipOS.write(data);
            gzipOS.finish();
            return bos.toByteArray();
        }
    }
    
    private byte[] decompressGzip(byte[] data) throws Exception {
        try (GZIPInputStream gzipIS = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIS.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }
    
    private byte[] encrypt(byte[] data) throws Exception {
        return cipher.doFinal(data);
    }
    
    private byte[] decrypt(byte[] data) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }
    
    public StorageMetrics getMetrics() {
        return metrics;
    }
    
    public void shutdown() {
        compressionExecutor.shutdown();
    }
    
    private boolean containsSensitiveData(byte[] data) {
        // 实现敏感数据检测逻辑
        return false;
    }
    
    private byte[] compressLz4(byte[] data) {
        // 实现LZ4压缩逻辑
        return data;
    }
    
    private byte[] decompressLz4(byte[] data) {
        // 实现LZ4解压缩逻辑
        return data;
    }
    
    private void initializeCipher() {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize cipher", e);
        }
    }
} 