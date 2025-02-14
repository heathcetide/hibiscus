package hibiscus.cetide.app.util.robust;

public class test {
    public static void main(String[] args) {
        RobustnessHandler<String> retryHandler = new RetryHandler<>(3, 1000); // 最大重试 3 次，间隔 1 秒
        retryHandler.setFallback(e -> "Fallback result: " + e.getMessage());

        RobustnessManager<String> manager = new RobustnessManager<>(retryHandler);

        try {
            String result = manager.execute(() -> {
                System.out.println("Trying to execute...");
                throw new RuntimeException("Simulated failure");
            });
            System.out.println("Result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }


        GlobalRobustnessManager.registerHandler("serviceA", new RetryHandler<>(3, 500));
        GlobalRobustnessManager.registerHandler("serviceB", new CircuitBreakerHandler<>(5, 3000));

        CompositeHandler<String> compositeHandler = new CompositeHandler<>();
        compositeHandler.addHandler(new RetryHandler<>(3, 500));
        compositeHandler.addHandler(new CircuitBreakerHandler<>(3, 3000));

        GlobalRobustnessManager.registerHandler("compositeService", compositeHandler);

        RobustnessHandler<String> handler = GlobalRobustnessManager.getHandler("compositeService");

        try {
            String result = handler.execute(() -> {
                System.out.println("Executing task...");
                throw new RuntimeException("Simulated failure");
            });
            System.out.println("Result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
