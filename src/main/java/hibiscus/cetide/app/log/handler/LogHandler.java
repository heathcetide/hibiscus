package hibiscus.cetide.app.log.handler;

import hibiscus.cetide.app.log.core.LogLevel;

import java.util.Map;

public interface LogHandler {
    void handle(LogLevel level, String message, Map<String, String> context);
}
