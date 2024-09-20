package hibiscus.cetide.app.log;

import hibiscus.cetide.app.log.core.LogLevel;

import java.util.HashMap;
import java.util.Map;

public class LogStats {
    private final Map<LogLevel, Integer> logLevelCounts = new HashMap<>();

    public void updateStats(LogLevel level) {
        logLevelCounts.put(level, logLevelCounts.getOrDefault(level, 0) + 1);
    }

    public Map<LogLevel, Integer> getStats() {
        return logLevelCounts;
    }
}
