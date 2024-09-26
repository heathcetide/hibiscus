package hibiscus.cetide.app.basic.signal;

// 定义信号处理器接口
@FunctionalInterface
public interface SignalHandler {
    void handle(Object sender, Object... params);
}