package hibiscus.cetide.app.log;

import hibiscus.cetide.app.log.palce.LogArchiver;

import java.io.File;

public class ZipArchivingStrategy implements ArchivingStrategy {
    @Override
    public void archive(File sourceFile, File targetFile) {
        LogArchiver.archiveLogFile(sourceFile, targetFile);
    }
}