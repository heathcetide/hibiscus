package hibiscus.cetide.app.component.cache.predictor;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class AccessPredictor<K> {
    private final ConcurrentHashMap<K, AccessPattern> patterns;
    private final int patternLength;
    private final double threshold;
    
    private class AccessPattern {
        final K key;
        final LinkedList<K> sequence;
        final Map<List<K>, Integer> frequencies;
        
        public AccessPattern(K key) {
            this.key = key;
            this.sequence = new LinkedList<>();
            this.frequencies = new ConcurrentHashMap<>();
        }
        
        public void recordAccess(K nextKey) {
            synchronized (sequence) {
                sequence.addLast(nextKey);
                if (sequence.size() > patternLength) {
                    sequence.removeFirst();
                }
                if (sequence.size() >= 2) {
                    List<K> pattern = new ArrayList<>(sequence);
                    frequencies.merge(pattern, 1, Integer::sum);
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        public List<K> predictNext() {
            return frequencies.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .map(pattern -> pattern.get(pattern.size() - 1))
                .distinct()
                .collect(Collectors.toList());
        }
    }
    
    public AccessPredictor(int patternLength, double threshold) {
        this.patternLength = patternLength;
        this.threshold = threshold;
        this.patterns = new ConcurrentHashMap<>();
    }

    public void recordAccess(K key) {
        patterns.computeIfAbsent(key, k -> new AccessPattern(k))
               .recordAccess(key);
    }

    public void recordSequence(K key1, K key2) {
        patterns.computeIfAbsent(key1, k -> new AccessPattern(k))
               .recordAccess(key2);
    }

    public List<K> predictNextAccesses(K key) {
        AccessPattern pattern = patterns.get(key);
        if (pattern == null) {
            return Collections.emptyList();
        }

        return pattern.predictNext();
    }
} 