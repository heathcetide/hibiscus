package hibiscus.cetide.app.basic.log.handler.impl;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.handler.LogHandler;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;

public class NetworkLogHandler implements LogHandler {
    private Socket socket;
    private PrintWriter out;

    public NetworkLogHandler(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        out.println("[" + level + "] " + message + " - " + context);
    }

    public void close() {
        out.close();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
