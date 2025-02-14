package hibiscus.cetide.app.util.robust;

import java.util.ArrayList;
import java.util.List;

public class CompositeHandler<T> implements RobustnessHandler<T> {

    private List<RobustnessHandler<T>> handlers = new ArrayList<>();

    public void addHandler(RobustnessHandler<T> handler) {
        handlers.add(handler);
    }

    @Override
    public T execute(RobustAction<T> action) throws Exception {
        for (RobustnessHandler<T> handler : handlers) {
            try {
                return handler.execute(action);
            } catch (Exception e) {
                // 如果当前策略失败，尝试下一个
            }
        }
        throw new RuntimeException("All strategies failed");
    }

    @Override
    public void setFallback(RobustFallback<T> fallback) {
        handlers.forEach(handler -> handler.setFallback(fallback));
    }

    @Override
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        for (RobustnessHandler<T> handler : handlers) {
            status.append(handler.getStatus()).append("\n");
        }
        return status.toString();
    }

    @Override
    public void reset() {
        handlers.forEach(RobustnessHandler::reset);
    }
}