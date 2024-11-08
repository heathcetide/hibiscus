package hibiscus.cetide.app.basic.log.core;

import hibiscus.cetide.app.basic.log.handler.LogHandler;
import hibiscus.cetide.app.basic.log.handler.impl.ConsoleLogHandler;
import hibiscus.cetide.app.basic.log.handler.impl.FileLogHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Logger {

    private LogLevel level = LogLevel.INFO;
    private final List<LogHandler> handlers;
    private final Map<String, String> context = new HashMap<>();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    public Logger(List<LogHandler> handlers) {
        this.handlers = handlers;
        initializeHandlers();
    }

    private void initializeHandlers() {
        LogHandler consoleHandler = new ConsoleLogHandler();
        LogHandler fileHandler = new FileLogHandler(new File("app.log"));
        handlers.add(consoleHandler);
        handlers.add(fileHandler);
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    @Deprecated
    public void addHandler(LogHandler handler) {
        handlers.add(handler);
    }

    public void log(LogLevel level, String message, String... contexts) {
        if (level.ordinal() >= this.level.ordinal()) {
            // 设置上下文
            setContext(contexts);
            // 异步提交日志处理任务
            executorService.submit(() -> {
                for (LogHandler handler : handlers) {
                    handler.handle(level, message, context);
                }
                // 清除上下文
                clearContext();
            });
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }

    // 添加一个方法来设置 context
    public void setContext(String... contexts) {
        if (contexts != null && contexts.length % 2 == 0) {
            for (int i = 0; i < contexts.length; i += 2) {
                context.put(contexts[i], contexts[i + 1]);
            }
        }else if (contexts != null && contexts.length == 1) {
            context.put("message", contexts[0]);
        }
    }

    // 添加一个方法来清除 context
    public void clearContext() {
        context.clear();
    }
}
