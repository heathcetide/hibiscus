package hibiscus.cetide.app.log;

import hibiscus.cetide.app.log.compress.LogCompressor;

import java.io.File;

public class GZipBackupStrategy implements BackupStrategy {
    @Override
    public void backup(File sourceFile, File targetFile) {
        LogCompressor.compressLogFile(sourceFile);
    }
}