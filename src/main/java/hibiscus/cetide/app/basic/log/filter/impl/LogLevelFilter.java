package hibiscus.cetide.app.basic.log.filter.impl;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.filter.LogFilter;


public class LogLevelFilter implements LogFilter {
    private LogLevel minLevel;

    public LogLevelFilter(LogLevel minLevel) {
        this.minLevel = minLevel;
    }

    @Override
    public boolean accept(String logLine) {
        String levelStr = logLine.substring(1, logLine.indexOf(']'));
        LogLevel level = LogLevel.valueOf(levelStr);
        return level.compareTo(minLevel) >= 0;
    }
}


