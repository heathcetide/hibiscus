package hibiscus.cetide.app.component.signal;

public class SigHandlerEvent {
    private final int evType;  // 0: add, 1: remove
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

    @Override
    public String toString() {
        return String.format("SigHandlerEvent{type=%d, signal='%s', handler=%d}",
            evType, signalName, sigHandler.getId());
    }
} 