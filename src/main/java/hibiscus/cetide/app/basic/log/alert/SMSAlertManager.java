package hibiscus.cetide.app.basic.log.alert;

public class SMSAlertManager implements AlertManager {
    @Override
    public void sendAlert(String message) {
        // 发送短信告警
        System.out.println("SMS Alert: " + message);
    }
}