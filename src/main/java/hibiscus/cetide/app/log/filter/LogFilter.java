package hibiscus.cetide.app.log.filter;

public interface LogFilter {
    boolean accept(String logLine);
}