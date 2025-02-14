package hibiscus.cetide.app.util.robust;

/**
 * 具体业务逻辑的接口。
 */
@FunctionalInterface
public interface RobustAction<T> {
    T run() throws Exception;
}