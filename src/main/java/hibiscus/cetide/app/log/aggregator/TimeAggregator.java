package hibiscus.cetide.app.log.aggregator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeAggregator {
    private Map<String, Integer> timeCountMap = new HashMap<>();
    private Pattern pattern = Pattern.compile("\\[(\\w+)\\] (.*) - \\{.*}");

    public void aggregate(List<String> logLines) {
        for (String line : logLines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String timestamp = extractTimestamp(matcher.group(2));
                timeCountMap.put(timestamp, timeCountMap.getOrDefault(timestamp, 0) + 1);
            }
        }
    }

    public Map<String, Integer> getTimeCounts() {
        return timeCountMap;
    }

    private String extractTimestamp(String line) {
        // 假设日志格式为 YYYY-MM-DD HH:MM:SS
        String[] parts = line.split(" ");
        return parts[0] + " " + parts[1];
    }
}
