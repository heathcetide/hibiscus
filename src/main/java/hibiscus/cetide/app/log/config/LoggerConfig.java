package hibiscus.cetide.app.log.config;

import hibiscus.cetide.app.log.core.LogLevel;

import java.util.Properties;

public class LoggerConfig {
    private LogLevel level = LogLevel.INFO;

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public void loadFromProperties(Properties properties) {
        String levelStr = properties.getProperty("log.level", "INFO");
        level = LogLevel.valueOf(levelStr.toUpperCase());
    }

    public void loadFromEnvironment() {
        String levelStr = System.getenv("LOG_LEVEL");
        if (levelStr != null) {
            level = LogLevel.valueOf(levelStr.toUpperCase());
        }
    }
}
