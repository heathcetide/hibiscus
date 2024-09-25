package hibiscus.cetide.app.basic.log.handler.impl;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.handler.LogHandler;

import java.util.Map;

public class AlertLogHandler implements LogHandler {
    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        if (level == LogLevel.ERROR || level == LogLevel.CRITICAL) {
            // 触发告警
            System.err.println("ALERT: [" + level + "] " + message + " - " + context);
        }
    }
}
