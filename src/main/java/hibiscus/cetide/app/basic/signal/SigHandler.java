package hibiscus.cetide.app.basic.signal;

// 定义信号处理器结构体
public class SigHandler {
    private final int id;
    private final SignalHandler handler;

    public SigHandler(int id, SignalHandler handler) {
        this.id = id;
        this.handler = handler;
    }

    public int getId() {
        return id;
    }

    public SignalHandler getHandler() {
        return handler;
    }
}