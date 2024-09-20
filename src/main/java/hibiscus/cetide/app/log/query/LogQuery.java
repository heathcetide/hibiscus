package hibiscus.cetide.app.log.query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LogQuery {
    private List<String> logLines = new ArrayList<>();
    private Pattern pattern;

    public void addLogLine(String line) {
        logLines.add(line);
    }

    public void setPattern(String regex) {
        pattern = Pattern.compile(regex);
    }

    public List<String> queryLogs() {
        List<String> matchingLines = new ArrayList<>();
        for (String line : logLines) {
            if (pattern.matcher(line).find()) {
                matchingLines.add(line);
            }
        }
        return matchingLines;
    }
}
