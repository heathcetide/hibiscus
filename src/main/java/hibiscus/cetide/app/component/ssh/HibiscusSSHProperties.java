package hibiscus.cetide.app.component.ssh;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hibiscus.ssh")
public class HibiscusSSHProperties {
    private int connectionTimeout = 30000;
    private int channelTimeout = 30000;
    private boolean strictHostKeyChecking = false;
    private String defaultTerminalType = "xterm";
    private int defaultCols = 80;
    private int defaultRows = 24;

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getChannelTimeout() {
        return channelTimeout;
    }

    public void setChannelTimeout(int channelTimeout) {
        this.channelTimeout = channelTimeout;
    }

    public boolean isStrictHostKeyChecking() {
        return strictHostKeyChecking;
    }

    public void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
        this.strictHostKeyChecking = strictHostKeyChecking;
    }

    public String getDefaultTerminalType() {
        return defaultTerminalType;
    }

    public void setDefaultTerminalType(String defaultTerminalType) {
        this.defaultTerminalType = defaultTerminalType;
    }

    public int getDefaultCols() {
        return defaultCols;
    }

    public void setDefaultCols(int defaultCols) {
        this.defaultCols = defaultCols;
    }

    public int getDefaultRows() {
        return defaultRows;
    }

    public void setDefaultRows(int defaultRows) {
        this.defaultRows = defaultRows;
    }
}