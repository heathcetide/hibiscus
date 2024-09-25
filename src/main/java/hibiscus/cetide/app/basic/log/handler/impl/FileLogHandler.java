package hibiscus.cetide.app.basic.log.handler.impl;

import hibiscus.cetide.app.basic.log.handler.LogHandler;
import hibiscus.cetide.app.basic.log.core.LogLevel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class FileLogHandler implements LogHandler {
    private File file;

    public FileLogHandler(File file) {
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