package hibiscus.cetide.app.basic.log;

import hibiscus.cetide.app.basic.log.core.LogLevel;

import java.util.Map;

public class LogFormatter {
    public static String format(LogLevel level, String message, Map<String, String> context) {
        return "[" + level + "] " + message + " - " + context;
    }

    public static String formatJson(LogLevel level, String message, Map<String, String> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"level\": \"").append(level).append("\", ");
        sb.append("\"message\": \"").append(message).append("\", ");
        sb.append("\"context\": {");

        boolean first = true;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            first = false;
        }

        sb.append(" }}");

        return sb.toString();
    }
}
