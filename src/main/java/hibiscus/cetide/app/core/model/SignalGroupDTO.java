package hibiscus.cetide.app.core.model;

public class SignalGroupDTO {
    private String name;
    private SignalConfigDTO defaultConfig;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SignalConfigDTO getDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(SignalConfigDTO defaultConfig) {
        this.defaultConfig = defaultConfig;
    }
}