package hibiscus.cetide.app.log.handler.impl;

import hibiscus.cetide.app.log.core.LogLevel;
import hibiscus.cetide.app.log.handler.LogHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class FileSystemLogHandler implements LogHandler {
    private File file;

    public FileSystemLogHandler(File file) {
        this.file = file;
    }

    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write("[" + level + "] " + message + " - " + context);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
