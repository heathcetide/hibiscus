package hibiscus.cetide.app.log.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogBackup {
    public static void backupLogFile(File file) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = dateFormat.format(new Date());
        File backupFile = new File(file.getParentFile(), file.getName() + ".backup." + timestamp);
        try {
            Files.copy(file.toPath(), backupFile.toPath());
            System.out.println("Log file backed up: " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to back up log file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }
}
