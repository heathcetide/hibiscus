package hibiscus.cetide.app.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class HibiscusHashMap<K, V> implements Map<K, V>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final int INITIAL_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    private static final int TREEIFY_THRESHOLD = 8;
    private static final int MIN_TREEIFY_CAPACITY = 64;

    private Node<K, V>[] table;
    private int size;
    private int threshold;

    public HibiscusHashMap() {
        this(INITIAL_CAPACITY);
    }

    public HibiscusHashMap(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " + initialCapacity);
        }
        this.table = new Node[initialCapacity];
        this.threshold = (int) (initialCapacity * LOAD_FACTOR);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return getNode(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Node<K, V> node : table) {
            while (node != null) {
                if (Objects.equals(node.value, value)) {
                    return true;
                }
                node = node.next;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        Node<K, V> node = getNode(key);
        return node != null ? node.value : null;
    }

    private Node<K, V> getNode(Object key) {
        int index = indexFor(hash(key));
        Node<K, V> node = table[index];
        while (node != null) {
            if (Objects.equals(node.key, key)) {
                return node;
            }
            node = node.next;
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        if (size >= threshold) {
            resize();
        }

        int index = indexFor(hash(key));
        Node<K, V> node = table[index];

        while (node != null) {
            if (Objects.equals(node.key, key)) {
                V oldValue = node.value;
                node.value = value;
                return oldValue;
            }
            node = node.next;
        }

        addNode(index, key, value);
        return null;
    }

    private void addNode(int index, K key, V value) {
        Node<K, V> newNode = new Node<>(key, value, table[index]);
        table[index] = newNode;
        size++;

        // Check if the current bucket needs to be treeified
        if (size >= TREEIFY_THRESHOLD && table.length >= MIN_TREEIFY_CAPACITY) {
            treeify(index);
        }
    }

    private void treeify(int index) {
        Node<K, V> node = table[index];
        // Create a new Red-Black tree with the existing nodes in the bucket
        TreeNode<K, V> treeRoot = null;
        while (node != null) {
            TreeNode<K, V> newTreeNode = new TreeNode<>(node.key, node.value, null, null);
            treeRoot = putTreeNode(treeRoot, newTreeNode);
            node = node.next;
        }
        table[index] = treeRoot;
    }

    private TreeNode<K, V> putTreeNode(TreeNode<K, V> root, TreeNode<K, V> newNode) {
        if (root == null) {
            return newNode;
        }
        int cmp = compare(newNode.key, root.key);
        if (cmp < 0) {
            root.left = putTreeNode(root.left, newNode);
        } else if (cmp > 0) {
            root.right = putTreeNode(root.right, newNode);
        } else {
            root.value = newNode.value;
        }
        return balance(root);
    }

    private int compare(K key1, K key2) {
        return key1.hashCode() - key2.hashCode(); // A basic comparison using hashCode (you could implement more complex comparison)
    }

    private TreeNode<K, V> balance(TreeNode<K, V> node) {
        // Implement Red-Black Tree balancing logic here
        return node; // Placeholder for balancing logic
    }

    private void resize() {
        Node<K, V>[] newTable = new Node[table.length * 2];
        threshold = (int) (newTable.length * LOAD_FACTOR);
        for (Node<K, V> node : table) {
            while (node != null) {
                int newIndex = indexFor(hash(node.key), newTable.length);
                Node<K, V> next = node.next;
                node.next = newTable[newIndex];
                newTable[newIndex] = node;
                node = next;
            }
        }
        table = newTable;
    }

    @Override
    public V remove(Object key) {
        int index = indexFor(hash(key));
        Node<K, V> node = table[index];
        Node<K, V> prev = null;

        while (node != null) {
            if (Objects.equals(node.key, key)) {
                if (prev == null) {
                    table[index] = node.next;
                } else {
                    prev.next = node.next;
                }
                size--;
                return node.value;
            }
            prev = node;
            node = node.next;
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        Arrays.fill(table, null);
        size = 0;
    }

    @Override
    public Set<K> keySet() {
        return null;
    }

    @Override
    public Collection<V> values() {
        return null;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }

    private int hash(Object key) {
        return key.hashCode();
    }

    private int indexFor(int hash) {
        return indexFor(hash, table.length);
    }

    private int indexFor(int hash, int length) {
        return (hash & (length - 1));
    }

    // Node类表示哈希表中的每个节点（一个键值对）
    static class Node<K, V> implements Map.Entry<K, V> {
        final K key;
        V value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }

    static class TreeNode<K, V> extends Node<K, V> {
        TreeNode<K, V> left;
        TreeNode<K, V> right;
        TreeNode<K, V> parent;

        TreeNode(K key, V value, TreeNode<K, V> left, TreeNode<K, V> right) {
            super(key, value, null);
            this.left = left;
            this.right = right;
        }
    }
}
