package hibiscus.cetide.app.util.robust;

public class RobustnessManager<T> {

    private RobustnessHandler<T> handler;

    public RobustnessManager(RobustnessHandler<T> handler) {
        this.handler = handler;
    }

    public T execute(RobustAction<T> action) throws Exception {
        return handler.execute(action);
    }

    public void setFallback(RobustFallback<T> fallback) {
        handler.setFallback(fallback);
    }

    public String getStatus() {
        return handler.getStatus();
    }

    public void reset() {
        handler.reset();
    }
}