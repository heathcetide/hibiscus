package hibiscus.cetide.app.basic.log.cleaner;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleLogCleaner implements LogCleaner {
    @Override
    public void clean(File directory, int daysToKeep) {
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000);
        List<File> filesToDelete = Stream.of(directory.listFiles())
                .filter(file -> file.lastModified() < cutoffTime)
                .collect(Collectors.toList());

        for (File file : filesToDelete) {
            if (file.delete()) {
                System.out.println("Deleted old log file: " + file.getAbsolutePath());
            } else {
                System.err.println("Failed to delete old log file: " + file.getAbsolutePath());
            }
        }
    }
}