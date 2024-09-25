package hibiscus.cetide.app.basic.log;

import hibiscus.cetide.app.basic.log.palce.LogArchiver;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class LogBackupScheduler {
    private File logFile;
    private File backupDirectory;
    private Timer timer;

    public LogBackupScheduler(File logFile, File backupDirectory, int intervalInSeconds) {
        this.logFile = logFile;
        this.backupDirectory = backupDirectory;
        this.timer = new Timer();
        scheduleBackup(intervalInSeconds);
    }

    private void scheduleBackup(int intervalInSeconds) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                File backupFile = new File(backupDirectory, logFile.getName() + ".bak");
                LogArchiver.archiveLogFile(logFile, backupFile);
            }
        }, 0, intervalInSeconds * 1000);
    }

    public void stopBackup() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
