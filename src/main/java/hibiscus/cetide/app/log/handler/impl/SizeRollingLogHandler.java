package hibiscus.cetide.app.log.handler.impl;

import hibiscus.cetide.app.log.core.LogLevel;
import hibiscus.cetide.app.log.handler.LogHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class SizeRollingLogHandler implements LogHandler {
    private File file;
    private long maxSize;

    public SizeRollingLogHandler(File file, long maxSize) {
        this.file = file;
        this.maxSize = maxSize;
    }

    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        if (file.length() > maxSize) {
            renameFile(file, new File(file.getParentFile(), file.getName() + ".old"));
            file = new File(file.getParentFile(), file.getName());
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write("[" + level + "] " + message + " - " + context);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renameFile(File oldFile, File newFile) {
        if (oldFile.renameTo(newFile)) {
            System.out.println("Log file rolled over: " + newFile.getAbsolutePath());
        } else {
            System.err.println("Failed to roll over log file: " + oldFile.getAbsolutePath());
        }
    }
}
