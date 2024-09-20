package hibiscus.cetide.app.log.handler.impl;

import hibiscus.cetide.app.log.core.LogLevel;
import hibiscus.cetide.app.log.handler.LogHandler;

import java.util.Map;

public class AuditLogHandler implements LogHandler {
    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        // 记录审计日志
        System.out.println("Audit Log: [" + level + "] " + message + " - " + context);
    }
}
