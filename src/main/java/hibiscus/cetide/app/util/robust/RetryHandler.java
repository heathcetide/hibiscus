package hibiscus.cetide.app.util.robust;
import java.util.concurrent.TimeUnit;

public class RetryHandler<T> implements RobustnessHandler<T> {

    private int maxRetries;          // 最大重试次数
    private long retryIntervalMs;    // 重试间隔时间
    private RobustFallback<T> fallback;

    public RetryHandler(int maxRetries, long retryIntervalMs) {
        this.maxRetries = maxRetries;
        this.retryIntervalMs = retryIntervalMs;
    }

    @Override
    public T execute(RobustAction<T> action) throws Exception {
        int attempts = 0;
        while (attempts <= maxRetries) {
            try {
                return action.run();
            } catch (Exception e) {
                attempts++;
                if (attempts > maxRetries) {
                    if (fallback != null) {
                        return fallback.fallback(e); // 执行降级逻辑
                    }
                    throw e; // 超过重试次数后抛出异常
                }
                TimeUnit.MILLISECONDS.sleep(retryIntervalMs);
            }
        }
        return null; // 不应该到达这里
    }

    @Override
    public void setFallback(RobustFallback<T> fallback) {
        this.fallback = fallback;
    }

    @Override
    public String getStatus() {
        return "RetryHandler: maxRetries=" + maxRetries + ", retryIntervalMs=" + retryIntervalMs;
    }

    @Override
    public void reset() {
        // 重试逻辑无需要重置状态
    }
}