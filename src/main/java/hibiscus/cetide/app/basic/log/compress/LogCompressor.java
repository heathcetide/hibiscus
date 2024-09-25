package hibiscus.cetide.app.basic.log.compress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class LogCompressor {
    public static void compressLogFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             FileOutputStream fos = new FileOutputStream(file + ".gz");
             GZIPOutputStream gzip = new GZIPOutputStream(fos)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                gzip.write(buffer, 0, length);
            }
            System.out.println("Log file compressed: " + file.getAbsolutePath() + ".gz");
        } catch (IOException e) {
            System.err.println("Failed to compress log file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }
}
