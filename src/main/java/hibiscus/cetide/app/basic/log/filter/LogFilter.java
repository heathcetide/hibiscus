package hibiscus.cetide.app.basic.log.filter;

public interface LogFilter {
    boolean accept(String logLine);
}