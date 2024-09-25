package hibiscus.cetide.app.basic.log.alert;

public class EmailAlertManager implements AlertManager {
    @Override
    public void sendAlert(String message) {
        // 发送邮件告警
        System.out.println("Email Alert: " + message);
    }
}