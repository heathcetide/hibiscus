package hibiscus.cetide.app.component.signal;

public interface SignalInterceptor {
    /**
     * 信号处理前的拦截
     * @param event 信号名称
     * @param sender 信号发送者
     * @param params 信号参数
     * @return true表示继续处理，false表示中断处理
     */
    default boolean beforeHandle(String event, Object sender, Object... params) {
        return true;
    }

    /**
     * 信号处理后的拦截
     * @param event 信号名称
     * @param sender 信号发送者
     * @param params 信号参数
     * @param error 处理过程中的异常，如果没有则为null
     */
    default void afterHandle(String event, Object sender, Object[] params, Throwable error) {
    }

    /**
     * 获取拦截器优先级
     * @return 优先级，数字越小优先级越高
     */
    default int getOrder() {
        return 0;
    }
} 