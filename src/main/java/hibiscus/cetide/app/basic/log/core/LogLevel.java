package hibiscus.cetide.app.basic.log.core;

public enum LogLevel {
    INFO("\u001B[34m"), // Green
    WARNING("\u001B[33m"), // Yellow
    ERROR("\u001B[31m"), // Red
    DEBUG("\u001B[32m"), // Blue
    CRITICAL("\u001B[35m"),
    START("\u001B[36m");
    private final String colorCode;

    LogLevel(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getColorCode() {
        return colorCode;
    }
}