package hibiscus.cetide.app.component.signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.io.File;

@Service("signalManager")
public class DefaultSignalManager implements SignalManager {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSignalManager.class);
    private final Signals signals;
    private final Map<String, SignalGroup> groups;
    private final SignalPersistence persistence;
    private final Map<String, Set<SignalFilter>> filters;
    private final Map<String, Set<SignalTransformer>> transformers;
    private final Set<SignalInterceptor> interceptors;
    private volatile boolean running;
    private volatile boolean debugMode;
    private final Object initLock = new Object();
    private static final String PERSISTENCE_DIR = "signal_data";
    public DefaultSignalManager() {
        this.signals = Signals.sig();
        this.groups = new ConcurrentHashMap<>();
        this.persistence = new SignalPersistence();
        this.filters = new ConcurrentHashMap<>();
        this.transformers = new ConcurrentHashMap<>();
        this.interceptors = new CopyOnWriteArraySet<>();
        this.running = false;
        this.debugMode = false;
    }

    @Override
    public void initialize() {
        synchronized (initLock) {
            if (!running) {
                running = true;
                loadState();
                logger.info("Signal manager initialized");
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public SignalContext createContext(String event, Object sender, Object... params) {
        return null;
    }

    @Override
    public void cancelSignal(String event) {

    }

    @Override
    public boolean isSignalCanceled(String event) {
        return false;
    }

    @Override
    public int connect(String event, SignalHandler handler) {
        checkRunning();
        return signals.connect(event, handler);
    }

    @Override
    public int connect(String event, SignalHandler handler, SignalConfig config) {
        checkRunning();
        if (config.isPersistent()) {
            persistence.saveSignalConfig(event, config);
        }
        return signals.connect(event, handler, config);
    }

    @Override
    public void disconnect(String event, int id) {
        checkRunning();
        signals.disconnect(event, id);
    }

    @Override
    public void emit(String event, Object sender, Object... params) {
        checkRunning();
        // 应用过滤器
        if (!applyFilters(event, sender, params)) {
            logger.debug("Signal filtered: {}", event);
            return;
        }

        // 应用转换器
        Object[] transformedParams = applyTransformers(event, sender, params);

        // 应用拦截器前置处理
        if (!applyInterceptorsBefore(event, sender, transformedParams)) {
            logger.debug("Signal intercepted: {}", event);
            return;
        }

        try {
            // 发送信号
            signals.emit(event, sender, transformedParams);
            // 应用拦截器后置处理
            applyInterceptorsAfter(event, sender, transformedParams, null);
        } catch (Exception e) {
            // 处理异常并应用拦截器后置处理
            applyInterceptorsAfter(event, sender, transformedParams, e);
            throw e;
        }
    }

    @Override
    public void emit(String event, Object sender, Consumer<Throwable> errorHandler, Object... params) {
        checkRunning();
        signals.emit(event, sender, errorHandler, params);
    }

    @Override
    public void emit(String event, Object sender, SignalCallback callback, Object... params) {

    }

    @Override
    public void emitWithContext(SignalContext context) {

    }

    @Override
    public void clear(String... events) {
        checkRunning();
        signals.clear(events);
        for (String event : events) {
            persistence.deleteSignalData(event);
        }
    }

    @Override
    public void createGroup(String groupName) {
        checkRunning();
        groups.putIfAbsent(groupName, new SignalGroup(groupName));
    }

    @Override
    public void createGroup(String groupName, SignalConfig defaultConfig) {
        checkRunning();
        groups.putIfAbsent(groupName, new SignalGroup(groupName, defaultConfig));
    }

    @Override
    public void addToGroup(String groupName, String signalName) {
        checkRunning();
        SignalGroup group = groups.get(groupName);
        if (group != null) {
            group.addSignal(signalName);
            SignalConfig config = group.getDefaultConfig();
            if (config != null) {
                updateConfig(signalName, config);
            }
        }
    }

    @Override
    public void removeFromGroup(String groupName, String signalName) {
        checkRunning();
        SignalGroup group = groups.get(groupName);
        if (group != null) {
            group.removeSignal(signalName);
        }
    }

    @Override
    public Set<String> getGroupSignals(String groupName) {
        checkRunning();
        SignalGroup group = groups.get(groupName);
        return group != null ? group.getSignals() : new HashSet<>();
    }

    @Override
    public void deleteGroup(String groupName) {
        checkRunning();
        groups.remove(groupName);
    }

    @Override
    public void updateConfig(String event, SignalConfig config) {
        checkRunning();
        signals.connect(event, (sender, params) -> {}, config);
        if (config.isPersistent()) {
            persistence.saveSignalConfig(event, config);
        }
    }

    @Override
    public SignalConfig getConfig(String event) {
        checkRunning();
        return persistence.loadSignalConfig(event);
    }

    @Override
    public void setPriority(String event, SignalPriority priority) {
        checkRunning();
        SignalConfig config = getConfig(event);
        if (config != null) {
            updateConfig(event, new SignalConfig.Builder()
                .async(config.isAsync())
                .maxRetries(config.getMaxRetries())
                .retryDelayMs(config.getRetryDelayMs())
                .maxHandlers(config.getMaxHandlers())
                .timeoutMs(config.getTimeoutMs())
                .recordMetrics(config.isRecordMetrics())
                .priority(priority)
                .groupName(config.getGroupName())
                .persistent(config.isPersistent())
                .build());
        }
    }

    @Override
    public SignalPriority getPriority(String event) {
        checkRunning();
        SignalConfig config = getConfig(event);
        return config != null ? config.getPriority() : SignalPriority.MEDIUM;
    }

    @Override
    public Map<String, Map<String, Object>> getAllMetrics() {
        checkRunning();
        return signals.getMetrics().getAllMetrics();
    }

    @Override
    public Map<String, Object> getMetrics(String event) {
        checkRunning();
        return signals.getMetrics().getMetrics(event);
    }

    @Override
    public void enableMetrics(String event) {
        checkRunning();
        SignalConfig config = getConfig(event);
        if (config != null) {
            updateConfig(event, new SignalConfig.Builder()
                .async(config.isAsync())
                .maxRetries(config.getMaxRetries())
                .retryDelayMs(config.getRetryDelayMs())
                .maxHandlers(config.getMaxHandlers())
                .timeoutMs(config.getTimeoutMs())
                .recordMetrics(true)
                .priority(config.getPriority())
                .groupName(config.getGroupName())
                .persistent(config.isPersistent())
                .build());
        }
    }

    @Override
    public void disableMetrics(String event) {
        checkRunning();
        SignalConfig config = getConfig(event);
        if (config != null) {
            updateConfig(event, new SignalConfig.Builder()
                .async(config.isAsync())
                .maxRetries(config.getMaxRetries())
                .retryDelayMs(config.getRetryDelayMs())
                .maxHandlers(config.getMaxHandlers())
                .timeoutMs(config.getTimeoutMs())
                .recordMetrics(false)
                .priority(config.getPriority())
                .groupName(config.getGroupName())
                .persistent(config.isPersistent())
                .build());
        }
    }

    @Override
    public void enablePersistence(String event) {
        checkRunning();
        SignalConfig config = getConfig(event);
        if (config != null) {
            updateConfig(event, new SignalConfig.Builder()
                .async(config.isAsync())
                .maxRetries(config.getMaxRetries())
                .retryDelayMs(config.getRetryDelayMs())
                .maxHandlers(config.getMaxHandlers())
                .timeoutMs(config.getTimeoutMs())
                .recordMetrics(config.isRecordMetrics())
                .priority(config.getPriority())
                .groupName(config.getGroupName())
                .persistent(true)
                .build());
        }
    }

    @Override
    public void disablePersistence(String event) {
        checkRunning();
        SignalConfig config = getConfig(event);
        if (config != null) {
            updateConfig(event, new SignalConfig.Builder()
                .async(config.isAsync())
                .maxRetries(config.getMaxRetries())
                .retryDelayMs(config.getRetryDelayMs())
                .maxHandlers(config.getMaxHandlers())
                .timeoutMs(config.getTimeoutMs())
                .recordMetrics(config.isRecordMetrics())
                .priority(config.getPriority())
                .groupName(config.getGroupName())
                .persistent(false)
                .build());
            persistence.deleteSignalData(event);
        }
    }

    @Override
    public void saveState() {
        checkRunning();
        Map<String, Map<String, Object>> allMetrics = signals.getMetrics().getAllMetrics();
        allMetrics.forEach((event, metrics) -> {
            SignalConfig config = getConfig(event);
            if (config != null && config.isPersistent()) {
                persistence.saveSignalMetrics(event, metrics);
            }
        });
    }

    @Override
    public void loadState() {
        try {
            // 加载所有持久化的信号配置
            File signalDataDir = new File(PERSISTENCE_DIR);
            if (signalDataDir.exists() && signalDataDir.isDirectory()) {
                File[] configFiles = signalDataDir.listFiles((dir, name) -> name.endsWith("_signal_config.json"));
                if (configFiles != null) {
                    for (File configFile : configFiles) {
                        String signalName = configFile.getName().replace("_signal_config.json", "");
                        SignalConfig config = persistence.loadSignalConfig(signalName);
                        if (config != null) {
                            // 恢复信号配置
                            updateConfig(signalName, config);
                            
                            // 如果配置属于某个组，恢复组关系
                            if (config.getGroupName() != null) {
                                String groupName = config.getGroupName();
                                if (!groups.containsKey(groupName)) {
                                    createGroup(groupName);
                                }
                                addToGroup(groupName, signalName);
                            }

                            // 加载信号的指标数据
                            Map<String, Object> metrics = persistence.loadSignalMetrics(signalName);
                            if (metrics != null) {
                                // TODO: 恢复指标数据
                                logger.debug("Loaded metrics for signal: {}", signalName);
                            }
                        }
                    }
                }
            }
            logger.info("Signal state loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load signal state", e);
        }
    }

    @Override
    public void addFilter(String event, SignalFilter filter) {
        checkRunning();
        if (filter != null) {
            filters.computeIfAbsent(event, k -> new CopyOnWriteArraySet<>()).add(filter);
            logger.debug("Added filter for event: {}", event);
        }
    }

    @Override
    public void removeFilter(String event, SignalFilter filter) {
        checkRunning();
        Set<SignalFilter> eventFilters = filters.get(event);
        if (eventFilters != null && filter != null) {
            eventFilters.remove(filter);
            logger.debug("Removed filter for event: {}", event);
        }
    }

    @Override
    public void addTransformer(String event, SignalTransformer transformer) {
        checkRunning();
        if (transformer != null) {
            transformers.computeIfAbsent(event, k -> new CopyOnWriteArraySet<>()).add(transformer);
            logger.debug("Added transformer for event: {}", event);
        }
    }

    @Override
    public void removeTransformer(String event, SignalTransformer transformer) {
        checkRunning();
        Set<SignalTransformer> eventTransformers = transformers.get(event);
        if (eventTransformers != null && transformer != null) {
            eventTransformers.remove(transformer);
            logger.debug("Removed transformer for event: {}", event);
        }
    }

    @Override
    public void addInterceptor(SignalInterceptor interceptor) {
        checkRunning();
        if (interceptor != null) {
            interceptors.add(interceptor);
            // 按优先级排序
            List<SignalInterceptor> sortedInterceptors = new ArrayList<>(interceptors);
            sortedInterceptors.sort(Comparator.comparingInt(SignalInterceptor::getOrder));
            interceptors.clear();
            interceptors.addAll(sortedInterceptors);
            logger.debug("Added interceptor with order: {}", interceptor.getOrder());
        }
    }

    @Override
    public void removeInterceptor(SignalInterceptor interceptor) {
        checkRunning();
        if (interceptor != null) {
            interceptors.remove(interceptor);
            logger.debug("Removed interceptor");
        }
    }

    @Override
    public Set<SignalInterceptor> getInterceptors() {
        checkRunning();
        return new HashSet<>(interceptors);
    }

    @Override
    public void shutdown() {
        if (running) {
            saveState();
            signals.shutdown();
            running = false;
            logger.info("Signal manager shut down");
        }
    }

    @Override
    public void enableDebugMode() {
        this.debugMode = true;
        SignalLogger.setDebugMode(true);
    }

    @Override
    public void disableDebugMode() {
        this.debugMode = false;
        SignalLogger.setDebugMode(false);
    }

    @Override
    public boolean isDebugMode() {
        return debugMode;
    }

    @Override
    public void setLogLevel(String level) {
        SignalLogger.setLevel(level);
    }

    @Override
    public List<String> getAllGroups() {
        return new ArrayList<>(groups.keySet());
    }

    private void checkRunning() {
        if (!running) {
            synchronized (initLock) {
                if (!running) {
                    initialize();
                }
            }
        }
    }

    private boolean applyFilters(String event, Object sender, Object... params) {
        Set<SignalFilter> eventFilters = filters.get(event);
        if (eventFilters != null) {
            for (SignalFilter filter : eventFilters) {
                try {
                    if (!filter.filter(event, sender, params)) {
                        return false;
                    }
                } catch (Exception e) {
                    logger.error("Error applying filter for event: {}", event, e);
                    return false;
                }
            }
        }
        return true;
    }

    private Object[] applyTransformers(String event, Object sender, Object... params) {
        Object[] currentParams = params;
        Set<SignalTransformer> eventTransformers = transformers.get(event);
        if (eventTransformers != null) {
            for (SignalTransformer transformer : eventTransformers) {
                try {
                    currentParams = transformer.transform(event, sender, currentParams);
                } catch (Exception e) {
                    logger.error("Error applying transformer for event: {}", event, e);
                }
            }
        }
        return currentParams;
    }

    private boolean applyInterceptorsBefore(String event, Object sender, Object... params) {
        for (SignalInterceptor interceptor : interceptors) {
            try {
                if (!interceptor.beforeHandle(event, sender, params)) {
                    return false;
                }
            } catch (Exception e) {
                logger.error("Error in interceptor beforeHandle for event: {}", event, e);
                return false;
            }
        }
        return true;
    }

    private void applyInterceptorsAfter(String event, Object sender, Object[] params, Throwable error) {
        for (SignalInterceptor interceptor : interceptors) {
            try {
                interceptor.afterHandle(event, sender, params, error);
            } catch (Exception e) {
                logger.error("Error in interceptor afterHandle for event: {}", event, e);
            }
        }
    }
} 