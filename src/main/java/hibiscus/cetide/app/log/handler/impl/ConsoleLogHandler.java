package hibiscus.cetide.app.log.handler.impl;

import hibiscus.cetide.app.log.core.LogLevel;
import hibiscus.cetide.app.log.handler.LogHandler;

import java.util.Map;

public class ConsoleLogHandler implements LogHandler {
    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        String colorCode = level.getColorCode();
        System.out.println("[" + colorCode + level + "\u001B[0m] " + message + " - " + context);
    }
}
