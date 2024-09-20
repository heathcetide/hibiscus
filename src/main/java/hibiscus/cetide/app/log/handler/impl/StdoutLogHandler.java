package hibiscus.cetide.app.log.handler.impl;

import hibiscus.cetide.app.log.core.LogLevel;
import hibiscus.cetide.app.log.handler.LogHandler;

import java.util.Map;

public class StdoutLogHandler implements LogHandler {
    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        System.out.println("[" + level + "] " + message + " - " + context);
    }
}
