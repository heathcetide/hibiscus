package hibiscus.cetide.app.log.palce;

import hibiscus.cetide.app.log.core.LogLevel;
import hibiscus.cetide.app.log.core.Logger;

import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeLogger extends Logger {
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void log(LogLevel level, String message) {
        lock.lock();
        try {
            super.log(level, message);
        } finally {
            lock.unlock();
        }
    }
}
