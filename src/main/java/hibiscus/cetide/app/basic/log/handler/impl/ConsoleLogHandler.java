package hibiscus.cetide.app.basic.log.handler.impl;

import hibiscus.cetide.app.basic.log.handler.LogHandler;
import hibiscus.cetide.app.basic.log.core.LogLevel;

import java.util.Map;

public class ConsoleLogHandler implements LogHandler {
    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        String colorCode = level.getColorCode();
        System.out.println("[" + colorCode + level + "\u001B[0m] " + message + " - " + context);
    }
}
