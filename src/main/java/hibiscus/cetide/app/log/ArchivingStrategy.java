package hibiscus.cetide.app.log;

import java.io.File;

public interface ArchivingStrategy {
    void archive(File sourceFile, File targetFile);
}