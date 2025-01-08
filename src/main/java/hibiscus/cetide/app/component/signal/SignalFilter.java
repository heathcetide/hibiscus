package hibiscus.cetide.app.component.signal;

@FunctionalInterface
public interface SignalFilter {
    /**
     * 过滤信号
     * @param event 信号名称
     * @param sender 信号发送者
     * @param params 信号参数
     * @return true表示允许信号继续传播，false表示拦截信号
     */
    boolean filter(String event, Object sender, Object... params);
} 