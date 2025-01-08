package hibiscus.cetide.app.component.signal;

@FunctionalInterface
public interface SignalTransformer {
    /**
     * 转换信号参数
     * @param event 信号名称
     * @param sender 信号发送者
     * @param params 原始信号参数
     * @return 转换后的信号参数
     */
    Object[] transform(String event, Object sender, Object... params);
} 