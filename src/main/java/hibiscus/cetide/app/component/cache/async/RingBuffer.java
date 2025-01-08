package hibiscus.cetide.app.component.cache.async;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class RingBuffer<T> {
    private static final int BUFFER_PAD = 32;
    private final T[] buffer;
    private final int mask;
    private final PaddedAtomicLong producerSequence;
    private final PaddedAtomicLong consumerSequence;
    
    private static final class PaddedAtomicLong extends AtomicLong {
        private long p1, p2, p3, p4, p5, p6, p7;
        public PaddedAtomicLong() {
            super(0L);
        }
    }
    
    @SuppressWarnings("unchecked")
    public RingBuffer(int size) {
        int actualSize = Integer.highestOneBit(size - 1) << 1;
        this.buffer = (T[]) new Object[actualSize];
        this.mask = actualSize - 1;
        this.producerSequence = new PaddedAtomicLong();
        this.consumerSequence = new PaddedAtomicLong();
    }
    
    public boolean offer(T element) {
        long sequence;
        do {
            sequence = producerSequence.get();
            if (sequence - consumerSequence.get() >= buffer.length) {
                return false;
            }
        } while (!producerSequence.compareAndSet(sequence, sequence + 1));
        
        buffer[(int)(sequence & mask)] = element;
        return true;
    }

    public T poll() {
        long sequence = consumerSequence.get();
        if (sequence >= producerSequence.get()) {
            return null; // 缓冲区空
        }
        
        T element = buffer[(int)(sequence & mask)];
        buffer[(int)(sequence & mask)] = null;
        consumerSequence.lazySet(sequence + 1);
        return element;
    }

    public boolean isEmpty() {
        return producerSequence.get() == consumerSequence.get();
    }
} 