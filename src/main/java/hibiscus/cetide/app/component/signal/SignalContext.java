package hibiscus.cetide.app.component.signal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SignalContext {
    private final String event;
    private final Object sender;
    private Object[] params;
    private final Map<String, Object> attributes;
    private final long timestamp;
    private final SignalPriority priority;
    private final String groupName;
    private volatile boolean canceled;
    private Map<String, Object> signalContext;

    private Map<String, Object> intermediateValues; // 新添加的属性，用于存储中间值

    public SignalContext(Map<String, Object> signalContext, String event, Object sender, SignalPriority priority, String groupName) {
        this.event = event;
        this.sender = sender;
        this.signalContext = signalContext;
        this.attributes = new ConcurrentHashMap<>();
        this.intermediateValues = new ConcurrentHashMap<>(); // 初始化
        this.timestamp = System.currentTimeMillis();
        this.priority = priority;
        this.groupName = groupName;
        this.canceled = false;
    }
    public SignalContext(String event, Object sender, SignalPriority priority, String groupName, Object... params) {
        this.event = event;
        this.sender = sender;
        this.params = params;
        this.attributes = new ConcurrentHashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.priority = priority;
        this.groupName = groupName;
        this.canceled = false;
    }

    // 添加新的方法来操作中间值
    public void addIntermediateValue(String key, Object value) {
        intermediateValues.put(key, value);
    }

    public Object getIntermediateValue(String key) {
        return intermediateValues.get(key);
    }

    public Map<String, Object> getIntermediateValues() {
        return new ConcurrentHashMap<>(intermediateValues);
    }

    public void setIntermediateValues(Map<String, Object> values) {
        if (this.intermediateValues != null){
            this.intermediateValues.clear();
        }
        if (this.intermediateValues == null){
            this.intermediateValues = new ConcurrentHashMap<>();
        }
        if (values != null) {
            this.intermediateValues.putAll(values);
        }
    }

    public String getEvent() {
        return event;
    }

    public Object getSender() {
        return sender;
    }

    public Object[] getParams() {
        return params;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public Map<String, Object> getAttributes() {
        return new ConcurrentHashMap<>(attributes);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public SignalPriority getPriority() {
        return priority;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        this.canceled = true;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public Map<String, Object> getSignalContext() {
        return signalContext;
    }

    public void setSignalContext(Map<String, Object> signalContext) {
        this.signalContext = signalContext;
    }

    @Override
    public String toString() {
        return String.format("SignalContext{event='%s', priority=%s, group='%s', timestamp=%d, canceled=%s}",
            event, priority, groupName, timestamp, canceled);
    }
} 