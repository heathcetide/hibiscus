package hibiscus.cetide.app.log;

import hibiscus.cetide.app.log.palce.LogArchiver;

import java.io.File;

public class ZipBackupStrategy implements BackupStrategy {
    @Override
    public void backup(File sourceFile, File targetFile) {
        LogArchiver.archiveLogFile(sourceFile, targetFile);
    }
}
