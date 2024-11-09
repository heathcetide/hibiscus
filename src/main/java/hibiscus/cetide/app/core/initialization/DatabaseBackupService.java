package hibiscus.cetide.app.core.initialization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class DatabaseBackupService {

    @Value("${db.username:your_db_username}")
    private String dbUsername;

    @Value("${db.password:your_db_password}")
    private String dbPassword;

    @Value("${db.database:your_database_name}")
    private String databaseName;

    @Value("${db.backup.path:/path/to/backup}")
    private String backupPath;

    @Value("${hibiscus.db-open: false}")
    private boolean dbOpen;

    // 备份数据库
    public void backupDatabase() {
        try {
            String backupFileName = backupPath + "/backup_" + System.currentTimeMillis() + ".sql";
            String command = String.format("mysqldump -u%s -p%s %s -r %s", dbUsername, dbPassword, databaseName, backupFileName);
            Process process = Runtime.getRuntime().exec(command);

            int processComplete = process.waitFor();
            if (processComplete == 0) {
                System.out.println("Database backup successful to " + backupFileName);
            } else {
                System.err.println("Database backup failed.");
            }

        } catch (Exception e) {
            System.err.println("Error during database backup: " + e.getMessage());
        }
    }

    // 还原数据库
    public void restoreDatabase(String backupFileName) {
        try {
            String command = String.format("mysql -u%s -p%s %s < %s", dbUsername, dbPassword, databaseName, backupFileName);
            Process process = Runtime.getRuntime().exec(command);

            int processComplete = process.waitFor();
            if (processComplete == 0) {
                System.out.println("Database restore successful from " + backupFileName);
            } else {
                System.err.println("Database restore failed.");
            }

        } catch (Exception e) {
            System.err.println("Error during database restore: " + e.getMessage());
        }
    }


    // 定时任务：每天凌晨2点备份一次
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleDatabaseBackup() {
        if (dbOpen){
            backupDatabase();
        }
    }
}
