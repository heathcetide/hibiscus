package hibiscus.cetide.app.log;

public class BatchMonitoringSystem implements MonitoringSystem {
    @Override
    public void monitor(String logLine) {
        // 批量监控系统处理逻辑
        System.out.println("Batch Monitoring: " + logLine);
    }
}