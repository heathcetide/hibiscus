package hibiscus.cetide.app.component.ssh;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class HibiscusSSHConnector {
    private static final Logger log = LoggerFactory.getLogger(HibiscusSSHConnector.class);

    private final WebSocketSession webSocketSession;
    private final HibiscusSSHProperties sshProperties;
    private final String sessionId;
    private Session sshSession;
    private ChannelShell channel;
    private OutputStream outputStream;
    private volatile boolean connected = false;

    public HibiscusSSHConnector(WebSocketSession webSocketSession, HibiscusSSHProperties sshProperties, String sessionId) {
        this.webSocketSession = webSocketSession;
        this.sshProperties = sshProperties;
        this.sessionId = sessionId;
    }

    // 添加文件传输支持
    public void uploadFile(String localPath, String remotePath) throws JSchException, SftpException {
        ChannelSftp channelSftp = (ChannelSftp) sshSession.openChannel("sftp");
        channelSftp.connect();
        try {
            channelSftp.put(localPath, remotePath);
        } finally {
            channelSftp.disconnect();
        }
    }

    // 添加命令执行结果获取
    public String executeCommand(String command) throws JSchException, IOException {
        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
        channel.setCommand(command);

        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        channel.setOutputStream(responseStream);
        channel.connect();

        while (channel.isConnected()) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                break;
            }
        }

        channel.disconnect();
        // 修改这里，使用正确的toString()方法
        return responseStream.toString("UTF-8");
    }

    // 添加连接状态检查
    public boolean isConnected() {
        return connected && sshSession != null && sshSession.isConnected();
    }

    // 添加会话信息获取
    public Properties getSessionConfig() {
        Properties config = new Properties();
        if (sshSession != null) {
            // 获取特定的配置项
            config.put("StrictHostKeyChecking", sshSession.getConfig("StrictHostKeyChecking"));
            config.put("PreferredAuthentications", sshSession.getConfig("PreferredAuthentications"));
            config.put("Compression", sshSession.getConfig("Compression"));
        }
        return config;
    }

    // 添加终端环境变量设置
    public void setEnvironmentVariable(String name, String value) throws JSchException {
        if (channel != null) {
            channel.setEnv(name, value);
        }
    }

    // 添加连接方法
    public void connect(String host, int port, String username, String password) throws JSchException {
        try {
            JSch jsch = new JSch();
            sshSession = jsch.getSession(username, host, port);
            sshSession.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password");
            config.put("terminal.echo", "false");
            sshSession.setConfig(config);

            sshSession.connect(sshProperties.getConnectionTimeout());


//            channel = (ChannelShell) sshSession.openChannel("shell");
//            channel.setPtyType("xterm");
//            channel.setPty(true);
//
//            // 设置终端环境变量
//            channel.setEnv("TERM", "xterm");
//            channel.setEnv("LANG", "en_US.UTF-8");

            channel = (ChannelShell) sshSession.openChannel("shell");
            channel.setPtyType("dumb");
            channel.setPty(true);
            channel.setPtySize(sshProperties.getDefaultCols(),
                    sshProperties.getDefaultRows(),
                    sshProperties.getDefaultCols() * 8,
                    sshProperties.getDefaultRows() * 8);
        } catch (HibiscusSSHException e) {
            throw new HibiscusSSHException("ssh连接失败", username, e);
        }
        try {
            InputStream inputStream = channel.getInputStream();
            outputStream = channel.getOutputStream();

            // 启动读取线程
            new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int i;
                    while ((i = inputStream.read(buffer)) != -1) {
                        if (webSocketSession.isOpen()) {
                            String output = new String(buffer, 0, i);
                            webSocketSession.sendMessage(new TextMessage(output));
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error reading from SSH", e);
                }
            }).start();

            channel.connect();
            connected = true;
        } catch (IOException e) {
            throw new JSchException("Failed to setup shell channel: " + e.getMessage());
        }
    }

    public void sendCommand(String command) {
        try {
            if (outputStream != null) {
                outputStream.write(command.getBytes());
                outputStream.flush();
            }
        } catch (IOException e) {
            log.error("Error sending command", e);
        }
    }

    public void close() {
        if (channel != null) {
            channel.disconnect();
        }
        if (sshSession != null) {
            sshSession.disconnect();
        }
        connected = false;
    }

    public ChannelShell getChannel() {
        return this.channel;
    }
}