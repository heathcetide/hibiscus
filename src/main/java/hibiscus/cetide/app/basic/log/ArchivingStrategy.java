package hibiscus.cetide.app.basic.log;

import java.io.File;

public interface ArchivingStrategy {
    void archive(File sourceFile, File targetFile);
}