package hibiscus.cetide.app.component.cache.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheLogger.class);

    public static void logInfo(String message) {
        LOGGER.info(message);
    }

    public static void logWarning(String message) {
        LOGGER.warn(message);
    }

    public static void logError(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
} 