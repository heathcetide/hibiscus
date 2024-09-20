package hibiscus.cetide.app.log;

public class RealtimeMonitoringSystem implements MonitoringSystem {
    @Override
    public void monitor(String logLine) {
        // 实时监控系统处理逻辑
        System.out.println("Realtime Monitoring: " + logLine);
    }
}