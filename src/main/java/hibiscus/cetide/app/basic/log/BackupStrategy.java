package hibiscus.cetide.app.basic.log;

import java.io.File;

public interface BackupStrategy {
    void backup(File sourceFile, File targetFile);

}
