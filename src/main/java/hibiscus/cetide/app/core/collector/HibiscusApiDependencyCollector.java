package hibiscus.cetide.app.core.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HibiscusApiDependencyCollector {
    private static final Logger log = LoggerFactory.getLogger(HibiscusApiDependencyCollector.class);
    
    private final Map<String, Set<String>> dependencies = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> inverseDependencies = new ConcurrentHashMap<>();
    private final ThreadLocal<Stack<String>> callStack = ThreadLocal.withInitial(Stack::new);
    
    public void enterApi(String path) {
        Stack<String> stack = callStack.get();
        if (!stack.isEmpty()) {
            String caller = stack.peek();
            addDependency(caller, path);
        }
        stack.push(path);
        log.debug("Entered API: {}", path);
    }
    
    public void exitApi(String path) {
        Stack<String> stack = callStack.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        log.debug("Exited API: {}", path);
    }
    
    private void addDependency(String caller, String callee) {
        dependencies.computeIfAbsent(caller, k -> ConcurrentHashMap.newKeySet()).add(callee);
        inverseDependencies.computeIfAbsent(callee, k -> ConcurrentHashMap.newKeySet()).add(caller);
        log.debug("Added dependency: {} -> {}", caller, callee);
    }
    
    public Set<String> getDependencies(String path) {
        return dependencies.getOrDefault(path, Collections.emptySet());
    }
    
    public Set<String> getInverseDependencies(String path) {
        return inverseDependencies.getOrDefault(path, Collections.emptySet());
    }
} 