package hibiscus.cetide.app.component.signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SignalLogger {
    private static final Logger logger = LoggerFactory.getLogger(SignalLogger.class);
    private static final AtomicReference<Level> currentLevel = new AtomicReference<>(Level.INFO);
    private static volatile boolean debugMode = false;

    public static void setLevel(String level) {
        try {
            currentLevel.set(Level.valueOf(level.toUpperCase()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid log level: {}. Using default level: {}", level, currentLevel.get());
        }
    }

    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        if (enabled) {
            currentLevel.set(Level.DEBUG);
        }
    }

    public static void logSignalEmit(SignalContext context) {
        if (shouldLog(Level.DEBUG)) {
            logger.debug("Signal emitted: {} [Priority: {}, Group: {}]", 
                context.getEvent(), context.getPriority(), context.getGroupName());
            if (debugMode) {
                logger.debug("Signal params: {}", Arrays.toString(context.getParams()));
            }
        }
    }

    public static void logSignalHandle(SignalContext context, long duration) {
        if (shouldLog(Level.DEBUG)) {
            logger.debug("Signal handled: {} [Duration: {}ms]", context.getEvent(), duration);
        }
    }

    public static void logSignalError(SignalContext context, Throwable error) {
        if (shouldLog(Level.ERROR)) {
            logger.error("Signal error: {} [{}]", context.getEvent(), error.getMessage(), error);
        }
    }

    public static void logSignalFilter(SignalContext context, boolean allowed) {
        if (shouldLog(Level.DEBUG)) {
            logger.debug("Signal filtered: {} [Allowed: {}]", context.getEvent(), allowed);
        }
    }

    public static void logSignalTransform(SignalContext context) {
        if (shouldLog(Level.DEBUG)) {
            logger.debug("Signal transformed: {}", context.getEvent());
            if (debugMode) {
                logger.debug("Transformed params: {}", Arrays.toString(context.getParams()));
            }
        }
    }

    public static void logSignalCancel(SignalContext context) {
        if (shouldLog(Level.INFO)) {
            logger.info("Signal canceled: {}", context.getEvent());
        }
    }

    public static void logConfigChange(String event, SignalConfig config) {
        if (shouldLog(Level.INFO)) {
            logger.info("Signal config updated: {} [Async: {}, Priority: {}]", 
                event, config.isAsync(), config.getPriority());
        }
    }

    public static void logMetricsUpdate(String event, Map<String, Object> metrics) {
        if (shouldLog(Level.DEBUG)) {
            logger.debug("Signal metrics updated: {} {}", event, metrics);
        }
    }

    public static void logSystemEvent(String message) {
        if (shouldLog(Level.INFO)) {
            logger.info("Signal system: {}", message);
        }
    }

    public static void logError(String message, Throwable error) {
        if (shouldLog(Level.ERROR)) {
            logger.error(message, error);
        }
    }

    private static boolean shouldLog(Level level) {
        return level.toInt() >= currentLevel.get().toInt();
    }
} 