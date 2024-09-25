package hibiscus.cetide.app.basic.log.handler.impl;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.handler.LogHandler;

import java.util.Map;

public class StderrLogHandler implements LogHandler {
    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        System.err.println("[" + level + "] " + message + " - " + context);
    }
}
