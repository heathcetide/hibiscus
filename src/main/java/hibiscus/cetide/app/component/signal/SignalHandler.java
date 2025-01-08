package hibiscus.cetide.app.component.signal;

@FunctionalInterface
public interface SignalHandler {
    void handle(Object sender, Object... params) throws InterruptedException;
} 