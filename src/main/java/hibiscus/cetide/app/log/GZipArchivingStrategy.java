package hibiscus.cetide.app.log;

import hibiscus.cetide.app.log.compress.LogCompressor;

import java.io.File;

public class GZipArchivingStrategy implements ArchivingStrategy {
    @Override
    public void archive(File sourceFile, File targetFile) {
        LogCompressor.compressLogFile(sourceFile);
    }
}