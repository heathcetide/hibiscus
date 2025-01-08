package hibiscus.cetide.app.component.signal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SignalGroup {
    private final String name;
    private final Set<String> signals;
    private final SignalConfig defaultConfig;

    public SignalGroup(String name) {
        this(name, new SignalConfig.Builder().build());
    }

    public SignalGroup(String name, SignalConfig defaultConfig) {
        this.name = name;
        this.signals = ConcurrentHashMap.newKeySet();
        this.defaultConfig = defaultConfig;
    }

    public void addSignal(String signalName) {
        signals.add(signalName);
    }

    public void removeSignal(String signalName) {
        signals.remove(signalName);
    }

    public Set<String> getSignals() {
        return new HashSet<>(signals);
    }

    public String getName() {
        return name;
    }

    public SignalConfig getDefaultConfig() {
        return defaultConfig;
    }
} 