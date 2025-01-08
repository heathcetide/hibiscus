package hibiscus.cetide.app.core.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 收集应用程序中API
 */
@Component
public class HibiscusApiDependencyCollector {
    private static final Logger log = LoggerFactory.getLogger(HibiscusApiDependencyCollector.class);
    
    private final Map<String, Set<String>> dependencies = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> inverseDependencies = new ConcurrentHashMap<>();
    private final ThreadLocal<Stack<String>> callStack = ThreadLocal.withInitial(Stack::new);

    /**
     * 标记API调用的进入点
     * @param path 进入的API路径。
     */
    public void enterApi(String path) {
        Stack<String> stack = callStack.get();
        if (!stack.isEmpty()) {
            String caller = stack.peek();
            addDependency(caller, path);
        }
        stack.push(path);
        log.debug("Entered API: {}", path);
    }

    /**
     * 标记API调用的退出点。
     *
     * @param path 退出的API路径。
     */
    public void exitApi(String path) {
        Stack<String> stack = callStack.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        log.debug("Exited API: {}", path);
    }

    /**
     * 添加API与被调用API
     *
     * @param caller 调用者API路径。
     * @param callee 被调用者API路径。
     */
    private void addDependency(String caller, String callee) {
        dependencies.computeIfAbsent(caller, k -> ConcurrentHashMap.newKeySet()).add(callee);
        inverseDependencies.computeIfAbsent(callee, k -> ConcurrentHashMap.newKeySet()).add(caller);
        log.debug("Added dependency: {} -> {}", caller, callee);
    }

    /**
     * 获取指定API调用的所有依赖API集合。
     *
     * @param path API路径。
     * @return 该API调用的所有依赖API集合。
     */
    public Set<String> getDependencies(String path) {
        return dependencies.getOrDefault(path, Collections.emptySet());
    }


    /**
     * 获取所有调用指定API的反向依赖API集合。
     *
     * @param path API路径
     * @return 所有调用该API的API集合。
     */
    public Set<String> getInverseDependencies(String path) {
        return inverseDependencies.getOrDefault(path, Collections.emptySet());
    }
} 