package hibiscus.cetide.app.component.signal;

public interface SignalCallback {
    /**
     * 信号处理成功的回调
     * @param event 信号名称
     * @param sender 信号发送者
     * @param params 信号参数
     */
    default void onSuccess(String event, Object sender, Object... params) {
    }

    /**
     * 信号处理失败的回调
     * @param event 信号名称
     * @param sender 信号发送者
     * @param error 错误信息
     * @param params 信号参数
     */
    default void onError(String event, Object sender, Throwable error, Object... params) {
    }

    /**
     * 信号处理完成的回调（无论成功失败）
     * @param event 信号名称
     * @param sender 信号发送者
     * @param params 信号参数
     */
    default void onComplete(String event, Object sender, Object... params) {
    }
} 