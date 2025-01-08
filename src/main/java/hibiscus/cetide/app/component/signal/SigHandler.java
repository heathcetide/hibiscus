package hibiscus.cetide.app.component.signal;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SigHandler that = (SigHandler) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
} 