package hibiscus.cetide.app.component.signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Signals {
    private static final Signals SIG = new Signals();
    private final Map<String, List<SigHandler>> sigHandlers = new ConcurrentHashMap<>();
    private final Map<String, SignalConfig> signalConfigs = new ConcurrentHashMap<>();
    private final SignalMetrics metrics = new SignalMetrics();
    private final ExecutorService executorService;
    private boolean inLoop = false;
    private final List<SigHandlerEvent> events = new ArrayList<>();

    private Signals() {
        // 创建线程池
        ThreadFactory threadFactory = new ThreadFactory() {
            private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = defaultFactory.newThread(r);
                thread.setName("Signal-Handler-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            }
        };
        this.executorService = new ThreadPoolExecutor(
            4, 
            Runtime.getRuntime().availableProcessors() * 2,
            60L, 
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public static Signals sig() {
        return SIG;
    }

    public synchronized void processEvents() {
        if (events.isEmpty() || inLoop) return;

        try {
            for (SigHandlerEvent event : events) {
                List<SigHandler> sigs = sigHandlers.computeIfAbsent(event.getSignalName(), k -> new CopyOnWriteArrayList<>());
                SignalConfig config = signalConfigs.computeIfAbsent(event.getSignalName(), 
                    k -> new SignalConfig.Builder().build());

                switch (event.getEvType()) {
                    case 0: // evTypeAdd
                        if (sigs.size() < config.getMaxHandlers()) {
                            sigs.add(event.getSigHandler());
                            if (config.isRecordMetrics()) {
                                metrics.recordHandlerAdded(event.getSignalName());
                            }
                        }
                        break;
                    case 1: // evTypeRemove
                        sigs.removeIf(sh -> sh.getId() == event.getSigHandler().getId());
                        if (config.isRecordMetrics()) {
                            metrics.recordHandlerRemoved(event.getSignalName());
                        }
                        break;
                }
            }
        } finally {
            events.clear();
        }
    }

    public int connect(String event, SignalHandler handler) {
        return connect(event, handler, new SignalConfig.Builder().build());
    }

    public int connect(String event, SignalHandler handler, SignalConfig config) {
        signalConfigs.put(event, config);
        int id = generateHandlerId();
        SigHandler sigHandler = new SigHandler(id, handler);
        SigHandlerEvent ev = new SigHandlerEvent(0, event, sigHandler);
        events.add(ev);
        processEvents();
        return id;
    }

    public void disconnect(String event, int id) {
        SigHandlerEvent ev = new SigHandlerEvent(1, event, new SigHandler(id, null));
        events.add(ev);
        processEvents();
    }

    public void clear(String... events) {
        for (String event : events) {
            sigHandlers.remove(event);
            signalConfigs.remove(event);
        }
    }

    public void emit(String event, Object sender, Object... params) {
        emit(event, sender, null, params);
    }

    public void emit(String event, Object sender, Consumer<Throwable> errorHandler, Object... params) {
        SignalConfig config = signalConfigs.getOrDefault(event, new SignalConfig.Builder().build());
        if (config.isRecordMetrics()) {
            metrics.recordEmit(event);
        }

        List<SigHandler> sigs = sigHandlers.get(event);
        if (sigs == null) return;

        if (config.isAsync()) {
            emitAsync(event, sender, sigs, config, errorHandler, params);
        } else {
            emitSync(event, sender, sigs, config, errorHandler, params);
        }
    }

    private void emitAsync(String event, Object sender, List<SigHandler> sigs, 
                          SignalConfig config, Consumer<Throwable> errorHandler, Object... params) {
        for (SigHandler sig : sigs) {
            CompletableFuture.runAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    executeWithRetry(event, sig, sender, config, params);
                    if (config.isRecordMetrics()) {
                        metrics.recordProcessingTime(event, System.currentTimeMillis() - startTime);
                    }
                } catch (Exception e) {
                    handleError(event, config, errorHandler, e);
                }
            }, executorService);
        }
    }

    private void emitSync(String event, Object sender, List<SigHandler> sigs,
                         SignalConfig config, Consumer<Throwable> errorHandler, Object... params) {
        inLoop = true;
        try {
            for (SigHandler sig : sigs) {
                long startTime = System.currentTimeMillis();
                try {
                    executeWithRetry(event, sig, sender, config, params);
                    if (config.isRecordMetrics()) {
                        metrics.recordProcessingTime(event, System.currentTimeMillis() - startTime);
                    }
                } catch (Exception e) {
                    handleError(event, config, errorHandler, e);
                }
            }
        } finally {
            inLoop = false;
            processEvents();
        }
    }

    private void executeWithRetry(String event, SigHandler sig, Object sender, 
                                SignalConfig config, Object... params) throws Exception {
        int retries = 0;
        Exception lastException = null;

        while (retries <= config.getMaxRetries()) {
            try {
                if (config.getTimeoutMs() > 0) {
                    executeWithTimeout(sig, sender, config.getTimeoutMs(), params);
                } else {
                    sig.getHandler().handle(sender, params);
                }
                return;
            } catch (Exception e) {
                lastException = e;
                retries++;
                if (retries <= config.getMaxRetries()) {
                    Thread.sleep(config.getRetryDelayMs());
                }
            }
        }

        if (lastException != null) {
            throw lastException;
        }
    }

    private void executeWithTimeout(SigHandler sig, Object sender, long timeoutMs, Object... params) 
            throws Exception {
        Future<?> future = executorService.submit(() -> {
            sig.getHandler().handle(sender, params);
            return null;
        });

        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new Exception("Signal handler execution timed out", e);
        } catch (ExecutionException e) {
            throw new Exception("Signal handler execution failed", e.getCause());
        }
    }

    private void handleError(String event, SignalConfig config, 
                           Consumer<Throwable> errorHandler, Exception e) {
        if (config.isRecordMetrics()) {
            metrics.recordError(event);
        }
        if (errorHandler != null) {
            errorHandler.accept(e);
        }
    }

    private synchronized int generateHandlerId() {
        return ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
    }

    public SignalMetrics getMetrics() {
        return metrics;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 