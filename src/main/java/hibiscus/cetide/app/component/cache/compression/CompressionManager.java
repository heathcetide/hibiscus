package hibiscus.cetide.app.component.cache.compression;

import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.nio.ByteBuffer;

public class CompressionManager {
    private final ThreadLocal<Deflater> deflater;
    private final ThreadLocal<Inflater> inflater;
    private final int compressionThreshold;
    private final int compressionLevel;
    
    public CompressionManager(int compressionThreshold, int compressionLevel) {
        this.compressionThreshold = compressionThreshold;
        this.compressionLevel = compressionLevel;
        this.deflater = ThreadLocal.withInitial(() -> {
            Deflater def = new Deflater(compressionLevel);
            def.setStrategy(Deflater.FILTERED);
            return def;
        });
        this.inflater = ThreadLocal.withInitial(Inflater::new);
    }
    
    public byte[] compress(byte[] data) {
        if (data.length < compressionThreshold) {
            return data;
        }
        
        Deflater def = deflater.get();
        try {
            def.setInput(data);
            def.finish();
            
            ByteBuffer buffer = ByteBuffer.allocate(data.length);
            while (!def.finished()) {
                int count = def.deflate(buffer.array(), buffer.position(), buffer.remaining());
                buffer.position(buffer.position() + count);
            }
            
            return Arrays.copyOf(buffer.array(), buffer.position());
        } finally {
            def.reset();
        }
    }
    
    public byte[] decompress(byte[] data) {
        if (data.length < compressionThreshold) {
            return data;
        }
        
        Inflater inf = inflater.get();
        try {
            inf.setInput(data);
            ByteBuffer buffer = ByteBuffer.allocate(data.length * 2);
            
            while (!inf.finished()) {
                int count = inf.inflate(buffer.array(), buffer.position(), buffer.remaining());
                if (count == 0 && inf.needsInput()) {
                    break;
                }
                buffer.position(buffer.position() + count);
            }
            
            return Arrays.copyOf(buffer.array(), buffer.position());
        } catch (Exception e) {
            throw new RuntimeException("Decompression failed", e);
        } finally {
            inf.reset();
        }
    }
} 