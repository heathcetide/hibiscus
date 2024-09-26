package hibiscus.cetide.app.basic.signal;

// 定义信号处理器事件结构体
public class SigHandlerEvent {
    private final int evType;
    private final String signalName;
    private final SigHandler sigHandler;

    public SigHandlerEvent(int evType, String signalName, SigHandler sigHandler) {
        this.evType = evType;
        this.signalName = signalName;
        this.sigHandler = sigHandler;
    }

    public int getEvType() {
        return evType;
    }

    public String getSignalName() {
        return signalName;
    }

    public SigHandler getSigHandler() {
        return sigHandler;
    }
}