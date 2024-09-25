package hibiscus.cetide.app.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ExpiredLRUCache<K, V> {
  private int capacity = 1000;
  private final Map<K, Node<K, V>> cache = new HashMap<>();
  private Node<K, V> first;
  private Node<K, V> last;
  private final long expiredTimeMillis;
  private final ScheduledExecutorService scheduler;

  public static class Node<K, V> {
    private K key;
    private V value;
    private Node<K, V> prev;
    private Node<K, V> next;
    private long timestamp;

    public Node(K key, V value) {
      this.key = key;
      this.value = value;
      this.timestamp = System.currentTimeMillis();
    }

    public K getKey() {
      return key;
    }

    public V getContent() {
      return value;
    }

    public void setContent(V value) {
      this.value = value;
    }

    public Node<K, V> getPrev() {
      return prev;
    }

    public void setPrev(Node<K, V> prev) {
      this.prev = prev;
    }

    public Node<K, V> getNext() {
      return next;
    }

    public void setNext(Node<K, V> next) {
      this.next = next;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public boolean isExpired(long expiredTimeMillis) {
      return System.currentTimeMillis() - timestamp > expiredTimeMillis;
    }
  }

  public ExpiredLRUCache(int capacity,long expiredTimeMillis) {
    this.capacity = capacity;
    this.expiredTimeMillis = expiredTimeMillis;
    this.scheduler = Executors.newSingleThreadScheduledExecutor();

    this.scheduler.scheduleAtFixedRate(this::cleanUpExpiredEntries, 0, 1, TimeUnit.SECONDS);
  }

  public V get(K key) {
    Node<K, V> node = cache.get(key);
    if (node != null && !node.isExpired(expiredTimeMillis)) {
      moveToHead(node);
      return node.getContent();
    } else {
      cache.remove(key);
      return null;
    }
  }

  public void put(K key, V value) {
    Node<K, V> node = cache.get(key);
    if (node != null) {
      node.setContent(value);
      moveToHead(node);
    } else {
      node = new Node<>(key, value);
      addToLinked(node);
      cache.put(key, node);
      if (cache.size() > capacity) {
        Node<K, V> removed = removeLast();
        cache.remove(removed.getKey());
      }
    }
  }

  private Node<K, V> removeLast() {
    Node<K, V> node = last;
    if (first == last) {
      first = null;
    }
    last = last.getPrev();
    if (last != null) {
      last.setNext(null);
    }
    node.setPrev(null);
    return node;
  }

  private void addToLinked(Node<K, V> node) {
    if (first == null) {
      first = last = node;
    } else {
      node.setNext(first);
      first.setPrev(node);
      first = node;
    }
  }

  private void moveToHead(Node<K, V> node) {
    if (node.getPrev() == null) return;
    if (node == last) {
      last = node.getPrev();
    }
    node.getPrev().setNext(node.getNext());
    if (node.getNext() != null) {
      node.getNext().setPrev(node.getPrev());
    }
    node.setPrev(null);
    node.setNext(first);
    first.setPrev(node);
    first = node;
  }

  private void cleanUpExpiredEntries() {
    cache.values().removeIf(node -> node.isExpired(expiredTimeMillis));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    Node<K, V> node = first;
    while (node != null) {
      sb.append(node.getKey() + ":" + node.getContent() + " , ");
      node = node.getNext();
    }
    return sb.toString();
  }

  // 遍历 HashMap 获取所有值
  public List<V> getAllValues() {
    List<V> values = new ArrayList<>();
    for (Node<K, V> node : cache.values()) {
      values.add(node.getContent());
    }
    return values;
  }

  // 遍历双向链表获取所有值
  public List<V> getAllValuesFromList() {
    List<V> values = new ArrayList<>();
    Node<K, V> node = first;
    while (node != null) {
      values.add(node.getContent());
      node = node.getNext();
    }
    return values;
  }
}
