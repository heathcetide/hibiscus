package hibiscus.cetide.app.basic.log;

import hibiscus.cetide.app.basic.log.palce.LogArchiver;

import java.io.File;

public class ZipArchivingStrategy implements ArchivingStrategy {
    @Override
    public void archive(File sourceFile, File targetFile) {
        LogArchiver.archiveLogFile(sourceFile, targetFile);
    }
}