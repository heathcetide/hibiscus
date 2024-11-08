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
        // 检查 logLine 是否符合期望的格式
        int endIndex = logLine.indexOf(']');
        if (endIndex <= 1) {  // 如果没有找到 ']' 或者 ']' 在第一个字符前
            return false;  // 格式不正确，直接返回 false
        }

        // 提取日志级别字符串
        String levelStr = logLine.substring(1, endIndex);
        LogLevel level;

        try {
            level = LogLevel.valueOf(levelStr);  // 转换为 LogLevel 枚举
        } catch (IllegalArgumentException e) {
            return false;  // 如果转换失败，则认为格式不正确
        }

        // 比较日志级别
        return level.compareTo(minLevel) >= 0;
    }

}


