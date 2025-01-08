package hibiscus.cetide.app.component.signal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public interface SignalManager {
    // 基本信号操作
    int connect(String event, SignalHandler handler);
    int connect(String event, SignalHandler handler, SignalConfig config);
    void disconnect(String event, int id);
    void emit(String event, Object sender, Object... params);
    void emit(String event, Object sender, Consumer<Throwable> errorHandler, Object... params);
    void emit(String event, Object sender, SignalCallback callback, Object... params);
    void emitWithContext(SignalContext context);
    void clear(String... events);

    // 信号组操作
    void createGroup(String groupName);
    void createGroup(String groupName, SignalConfig defaultConfig);
    void addToGroup(String groupName, String signalName);
    void removeFromGroup(String groupName, String signalName);
    Set<String> getGroupSignals(String groupName);
    void deleteGroup(String groupName);

    // 配置操作
    void updateConfig(String event, SignalConfig config);
    SignalConfig getConfig(String event);
    void setPriority(String event, SignalPriority priority);
    SignalPriority getPriority(String event);

    // 监控操作
    Map<String, Map<String, Object>> getAllMetrics();
    Map<String, Object> getMetrics(String event);
    void enableMetrics(String event);
    void disableMetrics(String event);

    // 持久化操作
    void enablePersistence(String event);
    void disablePersistence(String event);
    void saveState();
    void loadState();

    // 过滤和转换操作
    void addFilter(String event, SignalFilter filter);
    void removeFilter(String event, SignalFilter filter);
    void addTransformer(String event, SignalTransformer transformer);
    void removeTransformer(String event, SignalTransformer transformer);

    // 拦截器操作
    void addInterceptor(SignalInterceptor interceptor);
    void removeInterceptor(SignalInterceptor interceptor);
    Set<SignalInterceptor> getInterceptors();

    // 系统操作
    void shutdown();
    void initialize();
    boolean isRunning();

    // 上下文操作
    SignalContext createContext(String event, Object sender, Object... params);
    void cancelSignal(String event);
    boolean isSignalCanceled(String event);
    
    // 调试操作
    void enableDebugMode();
    void disableDebugMode();
    boolean isDebugMode();
    void setLogLevel(String level);

    List<String> getAllGroups();
} 