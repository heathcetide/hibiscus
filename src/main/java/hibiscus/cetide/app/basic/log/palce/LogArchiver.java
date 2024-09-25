package hibiscus.cetide.app.basic.log.palce;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogArchiver {
    public static void archiveLogFile(File logFile, File archiveFile) {
        try (FileInputStream fis = new FileInputStream(logFile);
             FileOutputStream fos = new FileOutputStream(archiveFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            ZipEntry zipEntry = new ZipEntry(logFile.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();

            System.out.println("Log file archived: " + archiveFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to archive log file: " + logFile.getAbsolutePath());
            e.printStackTrace();
        }
    }
}
