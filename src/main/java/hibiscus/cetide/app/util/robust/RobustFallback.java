package hibiscus.cetide.app.util.robust;

/**
 * 降级逻辑的接口。
 */
@FunctionalInterface
public interface RobustFallback<T> {
    T fallback(Exception e);
}
