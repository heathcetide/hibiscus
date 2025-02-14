package hibiscus.cetide.app.util.robust;

import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreakerHandler<T> implements RobustnessHandler<T> {

    private int failureThreshold;    // 失败次数阈值
    private long openDurationMs;     // 熔断器打开的持续时间
    private AtomicInteger failureCount = new AtomicInteger(0);
    private long openTimestamp = 0;  // 熔断器打开时间戳
    private RobustFallback<T> fallback;

    public CircuitBreakerHandler(int failureThreshold, long openDurationMs) {
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
    }

    @Override
    public T execute(RobustAction<T> action) throws Exception {
        if (isCircuitOpen()) {
            if (fallback != null) {
                return fallback.fallback(new RuntimeException("CircuitBreaker is OPEN"));
            }
            throw new RuntimeException("CircuitBreaker is OPEN");
        }

        try {
            T result = action.run();
            failureCount.set(0); // 成功时重置失败计数
            return result;
        } catch (Exception e) {
            if (failureCount.incrementAndGet() >= failureThreshold) {
                openTimestamp = System.currentTimeMillis(); // 打开熔断器
            }
            throw e;
        }
    }

    @Override
    public void setFallback(RobustFallback<T> fallback) {
        this.fallback = fallback;
    }

    @Override
    public String getStatus() {
        return "CircuitBreakerHandler: state=" + (isCircuitOpen() ? "OPEN" : "CLOSED") +
                ", failures=" + failureCount.get();
    }

    @Override
    public void reset() {
        failureCount.set(0);
        openTimestamp = 0;
    }

    private boolean isCircuitOpen() {
        if (failureCount.get() < failureThreshold) {
            return false;
        }
        return System.currentTimeMillis() - openTimestamp < openDurationMs;
    }
}