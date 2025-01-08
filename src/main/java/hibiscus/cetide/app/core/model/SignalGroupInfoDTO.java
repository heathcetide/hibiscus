package hibiscus.cetide.app.core.model;

public class SignalGroupInfoDTO {
    private String name;
    private int signalCount;

    public SignalGroupInfoDTO(String name, int signalCount) {
        this.name = name;
        this.signalCount = signalCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSignalCount() {
        return signalCount;
    }

    public void setSignalCount(int signalCount) {
        this.signalCount = signalCount;
    }
} 