package hibiscus.cetide.app.basic.log.handler.impl;

import hibiscus.cetide.app.basic.log.handler.LogHandler;
import hibiscus.cetide.app.basic.log.core.LogLevel;

import java.util.Map;

public class ConsoleLogHandler implements LogHandler {
    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        StringBuilder sb = new StringBuilder();
        String colorCode = level.getColorCode();
        sb.append("[").append(colorCode).append(level).append("\u001B[0m] ").append(message);

        if (!context.isEmpty()) {
            sb.append(" - ");
            context.forEach((k, v) -> sb.append(k).append(": ").append(v).append(", "));
            sb.setLength(sb.length() - 2); // 移除最后一个逗号和空格
        }

        System.out.println(sb);
    }
}
