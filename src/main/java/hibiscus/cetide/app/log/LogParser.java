package hibiscus.cetide.app.log;

import java.util.Map;

public interface LogParser {
    Map<String, String> parse(String logLine);
}
