package hibiscus.cetide.app.util.robust;

public class FallbackRobustnessHandler<T> implements RobustnessHandler<T> {

    private RobustFallback<T> fallback;

    public FallbackRobustnessHandler(RobustFallback<T> fallback) {
        this.fallback = fallback;
    }

    @Override
    public T execute(RobustAction<T> action) throws Exception {
        try {
            return action.run(); // 正常执行业务逻辑
        } catch (Exception e) {
            return fallback.fallback(e); // 执行降级逻辑
        }
    }

    @Override
    public void setFallback(RobustFallback<T> fallback) {
        this.fallback = fallback;
    }

    @Override
    public String getStatus() {
        return "FallbackHandler: active";
    }

    @Override
    public void reset() {
        // 降级策略无需特殊状态重置
    }
}