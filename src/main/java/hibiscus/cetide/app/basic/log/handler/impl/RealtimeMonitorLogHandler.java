package hibiscus.cetide.app.basic.log.handler.impl;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.handler.LogHandler;

import java.util.Map;

public class RealtimeMonitorLogHandler implements LogHandler {
    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        // 发送日志到实时监控系统
        System.out.println("Realtime Monitor: [" + level + "] " + message + " - " + context);
    }
}
