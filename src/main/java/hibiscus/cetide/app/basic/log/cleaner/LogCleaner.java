package hibiscus.cetide.app.basic.log.cleaner;

import java.io.File;

public interface LogCleaner {
    void clean(File directory, int daysToKeep);
}
