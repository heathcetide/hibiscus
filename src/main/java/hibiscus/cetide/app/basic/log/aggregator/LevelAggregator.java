package hibiscus.cetide.app.basic.log.aggregator;

import hibiscus.cetide.app.basic.log.core.LogLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LevelAggregator {
    private Map<LogLevel, Integer> levelCountMap = new HashMap<>();
    private Pattern pattern = Pattern.compile("\\[(\\w+)\\] (.*) - \\{.*}");

    public void aggregate(List<String> logLines) {
        for (String line : logLines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                LogLevel level = LogLevel.valueOf(matcher.group(1));
                levelCountMap.put(level, levelCountMap.getOrDefault(level, 0) + 1);
            }
        }
    }

    public Map<LogLevel, Integer> getLevelCounts() {
        return levelCountMap;
    }
}
