package hibiscus.cetide.app.component.cache.transaction;


import hibiscus.cetide.app.component.cache.HibiscusCache;
import hibiscus.cetide.app.component.cache.exception.CacheException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class TransactionManager<K, V> {
    private final ConcurrentHashMap<Long, Transaction<K, V>> activeTransactions;
    private final AtomicLong transactionIdGenerator;
    private final HibiscusCache<K, V> cache;
    private final int maxRetries;
    private final long timeoutMillis;
    
    public static class Transaction<K, V> {
        final long id;
        final Map<K, V> writeSet;
        final Set<K> readSet;
        final long startTime;
        volatile TransactionStatus status;
        
        public Transaction(long id) {
            this.id = id;
            this.writeSet = new ConcurrentHashMap<>();
            this.readSet = ConcurrentHashMap.newKeySet();
            this.startTime = System.currentTimeMillis();
            this.status = TransactionStatus.ACTIVE;
        }
    }
    
    public enum TransactionStatus {
        ACTIVE, COMMITTED, ABORTED
    }
    
    public TransactionManager(HibiscusCache<K, V> cache, int maxRetries, long timeoutMillis) {
        this.cache = cache;
        this.maxRetries = maxRetries;
        this.timeoutMillis = timeoutMillis;
        this.activeTransactions = new ConcurrentHashMap<>();
        this.transactionIdGenerator = new AtomicLong();
    }
    
    public long begin() {
        long txId = transactionIdGenerator.incrementAndGet();
        Transaction<K, V> tx = new Transaction<>(txId);
        activeTransactions.put(txId, tx);
        return txId;
    }
    
    public V get(long txId, K key) throws TransactionException, IOException, ClassNotFoundException, CacheException {
        Transaction<K, V> tx = getTransaction(txId);
        
        // 检查写集
        if (tx.writeSet.containsKey(key)) {
            return tx.writeSet.get(key);
        }
        
        // 检查其他事务的写集是否有冲突
        for (Transaction<K, V> other : activeTransactions.values()) {
            if (other.id != txId && other.writeSet.containsKey(key)) {
                throw new TransactionException("Write-read conflict detected");
            }
        }
        
        V value = cache.get(key);
        tx.readSet.add(key);
        return value;
    }
    
    public void put(long txId, K key, V value) throws TransactionException {
        Transaction<K, V> tx = getTransaction(txId);
        
        // 检查其他事务的读写集是否有冲突
        for (Transaction<K, V> other : activeTransactions.values()) {
            if (other.id != txId && 
                (other.readSet.contains(key) || other.writeSet.containsKey(key))) {
                throw new TransactionException("Write-write or read-write conflict detected");
            }
        }
        
        tx.writeSet.put(key, value);
    }
    
    public boolean commit(long txId) throws TransactionException {
        Transaction<K, V> tx = getTransaction(txId);
        
        // 验证事务
        if (!validate(tx)) {
            tx.status = TransactionStatus.ABORTED;
            activeTransactions.remove(txId);
            return false;
        }
        
        // 提交写集
        try {
            for (Map.Entry<K, V> entry : tx.writeSet.entrySet()) {
                cache.put(entry.getKey(), entry.getValue());
            }
            tx.status = TransactionStatus.COMMITTED;
            return true;
        } finally {
            activeTransactions.remove(txId);
        }
    }
    
    public void rollback(long txId) {
        Transaction<K, V> tx = activeTransactions.get(txId);
        if (tx != null) {
            tx.status = TransactionStatus.ABORTED;
            activeTransactions.remove(txId);
        }
    }
    
    private boolean validate(Transaction<K, V> tx) {
        // 检查事务是否超时
        if (System.currentTimeMillis() - tx.startTime > timeoutMillis) {
            return false;
        }
        
        // 验证读集
        for (K key : tx.readSet) {
            for (Transaction<K, V> other : activeTransactions.values()) {
                if (other.id != tx.id && 
                    other.writeSet.containsKey(key) && 
                    other.status == TransactionStatus.COMMITTED) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private Transaction<K, V> getTransaction(long txId) throws TransactionException {
        Transaction<K, V> tx = activeTransactions.get(txId);
        if (tx == null || tx.status != TransactionStatus.ACTIVE) {
            throw new TransactionException("Invalid transaction ID or transaction not active");
        }
        return tx;
    }
    
    public static class TransactionException extends Exception {
        public TransactionException(String message) {
            super(message);
        }
    }
} 