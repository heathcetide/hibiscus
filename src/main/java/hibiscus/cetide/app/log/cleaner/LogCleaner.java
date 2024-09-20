package hibiscus.cetide.app.log.cleaner;

import java.io.File;

public interface LogCleaner {
    void clean(File directory, int daysToKeep);
}
