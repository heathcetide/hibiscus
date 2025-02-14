package hibiscus.cetide.app.util.robust;

public interface RobustnessHandler<T> {

    /**
     * 执行带鲁棒性的任务。
     *
     * @param action 任务逻辑
     * @return 执行结果
     * @throws Exception 执行失败时抛出异常
     */
    T execute(RobustAction<T> action) throws Exception;

    /**
     * 设置降级逻辑。
     *
     * @param fallback 降级逻辑
     */
    void setFallback(RobustFallback<T> fallback);

    /**
     * 获取鲁棒性策略的当前状态。
     *
     * @return 当前状态
     */
    String getStatus();

    /**
     * 重置鲁棒性策略的状态。
     */
    void reset();
}

