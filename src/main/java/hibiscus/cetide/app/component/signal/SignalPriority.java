package hibiscus.cetide.app.component.signal;

public enum SignalPriority {
    HIGH(0),
    MEDIUM(1),
    LOW(2);

    private final int value;

    SignalPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
} 