package hibiscus.cetide.app.basic.log;

import hibiscus.cetide.app.basic.log.compress.LogCompressor;

import java.io.File;

public class GZipArchivingStrategy implements ArchivingStrategy {
    @Override
    public void archive(File sourceFile, File targetFile) {
        LogCompressor.compressLogFile(sourceFile);
    }
}