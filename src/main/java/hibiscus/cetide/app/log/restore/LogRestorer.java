package hibiscus.cetide.app.log.restore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class LogRestorer {
    public static void restoreLogFile(File compressedFile, File targetFile) {
        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(compressedFile));
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            System.out.println("Log file restored: " + targetFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to restore log file: " + compressedFile.getAbsolutePath());
            e.printStackTrace();
        }
    }
}
