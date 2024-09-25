package hibiscus.cetide.app.basic.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileAuditSystem implements AuditSystem {
    private File auditFile;

    public FileAuditSystem(File auditFile) {
        this.auditFile = auditFile;
    }

    @Override
    public void audit(String logLine) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(auditFile, true))) {
            writer.write(logLine);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}