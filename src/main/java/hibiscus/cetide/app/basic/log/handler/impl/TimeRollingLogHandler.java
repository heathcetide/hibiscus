package hibiscus.cetide.app.basic.log.handler.impl;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.handler.LogHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class TimeRollingLogHandler implements LogHandler {
    private File file;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    public TimeRollingLogHandler(File file) {
        this.file = file;
    }

    @Override
    public void handle(LogLevel level, String message, Map<String, String> context) {
        String currentDate = dateFormat.format(new Date());
        String fileName = file.getName();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.'));

        File newFile = new File(file.getParentFile(), baseName + currentDate + extension);

        if (!newFile.equals(file)) {
            renameFile(file, newFile);
            file = newFile;
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
