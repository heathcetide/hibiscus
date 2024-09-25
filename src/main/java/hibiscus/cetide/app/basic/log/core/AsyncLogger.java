package hibiscus.cetide.app.basic.log.core;

import hibiscus.cetide.app.basic.log.handler.LogHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncLogger extends Logger {

    private BlockingQueue<LogRecord> queue = new LinkedBlockingQueue<>(1000);
    private LogLevel level = LogLevel.INFO;
    private List<LogHandler> handlers = new ArrayList<>();
    private Map<String, String> context = new HashMap<>();
    private ExecutorService executorService;


    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public void addContext(String key, String value) {
        context.put(key, value);
    }

    public void addHandler(LogHandler handler) {
        handlers.add(handler);
    }

    public AsyncLogger() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::processLogs);
    }

    @Override
    public void log(LogLevel level, String message) {
        if (level.ordinal() >= this.level.ordinal()) {
            LogRecord record = new LogRecord(level, message, context);
            queue.offer(record);
        }
    }

    private void processLogs() {
        while (true) {
            try {
                LogRecord record = queue.take();
                for (LogHandler handler : handlers) {
                    handler.handle(record.getLevel(), record.getMessage(), record.getContext());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private static class LogRecord {
        private LogLevel level;
        private String message;
        private Map<String, String> context;

        public LogRecord(LogLevel level, String message, Map<String, String> context) {
            this.level = level;
            this.message = message;
            this.context = context;
        }

        public LogLevel getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, String> getContext() {
            return context;
        }
    }
}
