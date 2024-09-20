package hibiscus.cetide.app.log.core;


import hibiscus.cetide.app.log.backup.LogBackup;
import hibiscus.cetide.app.log.filter.LogFilter;
import hibiscus.cetide.app.log.filter.impl.LogLevelFilter;
import hibiscus.cetide.app.log.handler.impl.ConsoleLogHandler;
import hibiscus.cetide.app.log.handler.impl.FileLogHandler;
import hibiscus.cetide.app.log.handler.LogHandler;
import hibiscus.cetide.app.log.palce.LogArchiver;

import java.io.File;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        Logger logger = new Logger();
        logger.setLevel(LogLevel.INFO);
        logger.addContext("user", "admin");

        LogHandler consoleHandler = new ConsoleLogHandler();
        LogHandler fileHandler = new FileLogHandler(new File("app.log"));

        logger.addHandler(consoleHandler);
        logger.addHandler(fileHandler);

        logger.log(LogLevel.INFO, "Application started.");

        // 示例：异步日志记录
        for (int i = 0; i < 1000; i++) {
            logger.log(LogLevel.INFO, "Log message " + i);
        }

        // 示例：日志归档
        LogArchiver.archiveLogFile(new File("app.log"), new File("app.log.zip"));

        // 示例：日志备份
        LogBackup.backupLogFile(new File("app.log"));

        LogFilter logFilter = new LogLevelFilter(LogLevel.INFO);
        // 示例：日志过滤
        if (logFilter.accept(LogLevel.DEBUG+ "This is an error message")) {
            logger.log(LogLevel.INFO, "This is an error message");
        }

        // 关闭日志记录器
        logger.shutdown();
    }
}
