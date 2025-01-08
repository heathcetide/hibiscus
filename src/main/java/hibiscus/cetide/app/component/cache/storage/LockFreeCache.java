package hibiscus.cetide.app.component.cache.storage;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeCache<K, V> {
    private static final int MAX_LEVEL = 16;
    private final AtomicReference<Node<K, V>>[] heads;
    
    @SuppressWarnings("unchecked")
    public LockFreeCache() {
        heads = new AtomicReference[MAX_LEVEL];
        for (int i = 0; i < MAX_LEVEL; i++) {
            heads[i] = new AtomicReference<>(null);
        }
    }
    
    private static class Node<K, V> {
        final K key;
        volatile V value;
        final AtomicReference<Node<K, V>>[] next;
        
        @SuppressWarnings("unchecked")
        Node(K key, V value, int level) {
            this.key = key;
            this.value = value;
            next = new AtomicReference[level];
            for (int i = 0; i < level; i++) {
                next[i] = new AtomicReference<>(null);
            }
        }
    }
    
    public V put(K key, V value) {
        // 无锁插入实现
        Node<K, V> newNode = new Node<>(key, value, randomLevel());
        while (true) {
            if (tryInsert(newNode)) {
                return value;
            }
        }
    }

    private int randomLevel() {
        int level = 1;
        while (Math.random() < 0.5 && level < MAX_LEVEL) {
            level++;
        }
        return level;
    }

    private boolean tryInsert(Node<K, V> newNode) {
        Node<K, V>[] update = new Node[MAX_LEVEL];
        Node<K, V> current = heads[0].get();
        
        // 查找插入位置
        for (int i = MAX_LEVEL - 1; i >= 0; i--) {
            while (current != null && current.key.hashCode() < newNode.key.hashCode()) {
                current = current.next[i].get();
            }
            update[i] = current;
        }
        
        // 尝试插入
        Node<K, V> next = update[0] == null ? null : update[0].next[0].get();
        if (next != null && next.key.equals(newNode.key)) {
            next.value = newNode.value;
            return true;
        }
        
        // 插入新节点
        for (int i = 0; i < newNode.next.length; i++) {
            newNode.next[i].set(update[i] == null ? null : update[i].next[i].get());
            if (update[i] != null) {
                update[i].next[i].set(newNode);
            } else {
                heads[i].set(newNode);
            }
        }
        return true;
    }
} 