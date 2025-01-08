package hibiscus.cetide.app.core.service;

import hibiscus.cetide.app.core.model.monitor.*;
import org.springframework.stereotype.Service;
import java.lang.management.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class HibiscusMonitorService {
    
    private final ConcurrentHashMap<String, LinkedList<ApiMetrics.ApiInfo>> apiHistoryMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> pathTotalRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> pathTotalResponseTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> pathTotalErrors = new ConcurrentHashMap<>();
    private final int MAX_HISTORY_PER_PATH = 100;
    private final int MAX_PATHS = 50;

    // 连接池历史记录
    private final LinkedList<DbPoolMetrics.PoolInfo> poolHistory = new LinkedList<>();
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong idleConnections = new AtomicLong(0);
    private final int maxConnections = 20; // 默认最大连接数

    // 获取JVM监控指标
    public JvmMetrics getJvmMetrics() {
        JvmMetrics metrics = new JvmMetrics();
        
        // 获取内存管理器
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        // 计算堆内存使用率
        metrics.setHeapUsed(heapMemoryUsage.getUsed());
        metrics.setHeapMax(heapMemoryUsage.getMax());
        double heapUsage = heapMemoryUsage.getMax() > 0 
            ? ((double) heapMemoryUsage.getUsed() / heapMemoryUsage.getMax()) * 100 
            : 0;
        metrics.setHeapUsage(Math.round(heapUsage * 100.0) / 100.0);  // 保留两位小数

        // 计算非堆内存使用率
        metrics.setNonHeapUsed(nonHeapMemoryUsage.getUsed());
        // 对于非堆内存，使用committed而不是max，因为max可能返回-1
        long nonHeapCommitted = nonHeapMemoryUsage.getCommitted();
        double nonHeapUsage = nonHeapCommitted > 0 
            ? ((double) nonHeapMemoryUsage.getUsed() / nonHeapCommitted) * 100 
            : 0;
        metrics.setNonHeapUsage(Math.round(nonHeapUsage * 100.0) / 100.0);  // 保留两位小数

        // 获取GC信息
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long youngGcCount = 0;
        long fullGcCount = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String name = gcBean.getName().toLowerCase();
            if (name.contains("young") || name.contains("scavenge")) {
                youngGcCount += gcBean.getCollectionCount();
            } else if (name.contains("old") || name.contains("full")) {
                fullGcCount += gcBean.getCollectionCount();
            }
        }

        metrics.setYoungGcCount(youngGcCount);
        metrics.setFullGcCount(fullGcCount);

        return metrics;
    }

    // 获取线程监控指标
    public ThreadMetrics getThreadMetrics() {
        ThreadMetrics metrics = new ThreadMetrics();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        int activeThreads = threadMXBean.getThreadCount();
        metrics.setActiveThreads(activeThreads);
        metrics.setPeakThreads(threadMXBean.getPeakThreadCount());
        metrics.setDaemonThreads(threadMXBean.getDaemonThreadCount());

        // 获取线程历史记录
        List<ThreadMetrics.ThreadInfo> threadHistory = new ArrayList<>();
        ThreadMetrics.ThreadInfo info = new ThreadMetrics.ThreadInfo();
        info.setTimestamp(System.currentTimeMillis());
        info.setThreadCount(activeThreads);  // 使用当前活跃线程数
        threadHistory.add(info);
        
        metrics.setThreadHistory(threadHistory);
        return metrics;
    }

    // 获取数据库连接池监控指标
    public DbPoolMetrics getDbPoolMetrics() {
        DbPoolMetrics metrics = new DbPoolMetrics();
        
        // 使用原子变量获取当前值
        metrics.setActiveConnections((int) activeConnections.get());
        metrics.setIdleConnections((int) idleConnections.get());
        metrics.setMaxConnections(maxConnections);

        // 创建当前时刻的连接池信息
        DbPoolMetrics.PoolInfo currentInfo = new DbPoolMetrics.PoolInfo();
        currentInfo.setTimestamp(System.currentTimeMillis());
        currentInfo.setActiveCount(metrics.getActiveConnections());
        currentInfo.setIdleCount(metrics.getIdleConnections());

        // 添加到历史记录
        synchronized (poolHistory) {
            poolHistory.addFirst(currentInfo);
            while (poolHistory.size() > 100) { // 保持最近100条记录
                poolHistory.removeLast();
            }
            metrics.setConnectionHistory(new ArrayList<>(poolHistory));
        }

        return metrics;
    }

    // 更新连接池状态
    public void updatePoolStats(int active, int idle) {
        activeConnections.set(active);
        idleConnections.set(idle);
    }

    // 获取API监控指标
    public ApiMetrics getApiMetrics() {
        ApiMetrics metrics = new ApiMetrics();
        
        // 计算总体统计信息
        long totalReqs = pathTotalRequests.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
        
        long totalRespTime = pathTotalResponseTime.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
        
        long totalErrs = pathTotalErrors.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();

        metrics.setTotalRequests(totalReqs);
        
        // 计算平均响应时间
        if (totalReqs > 0) {
            metrics.setAvgResponseTime((double) totalRespTime / totalReqs);
        } else {
            metrics.setAvgResponseTime(0.0);
        }
        
        // 计算错误率
        if (totalReqs > 0) {
            metrics.setErrorRate(((double) totalErrs / totalReqs) * 100);
        } else {
            metrics.setErrorRate(0.0);
        }

        // 获取最近的API调用历史
        List<ApiMetrics.ApiInfo> allHistory = new ArrayList<>();
        apiHistoryMap.values().forEach(history -> {
            synchronized (history) {
                allHistory.addAll(history);
            }
        });

        // 按时间戳排序，最新的在前
        allHistory.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        
        // 只返回最近的100条记录
        metrics.setApiHistory(allHistory.size() > 100 ? 
            allHistory.subList(0, 100) : allHistory);

        // 添加每个路径的详细统计信息
        List<ApiMetrics.PathStats> pathStats = new ArrayList<>();
        for (String path : apiHistoryMap.keySet()) {
            ApiMetrics.PathStats stats = new ApiMetrics.PathStats();
            stats.setPath(path);
            
            // 安全地获取计数器值
            AtomicLong requestCounter = pathTotalRequests.get(path);
            AtomicLong responseTimeCounter = pathTotalResponseTime.get(path);
            AtomicLong errorCounter = pathTotalErrors.get(path);
            
            long pathReqs = requestCounter != null ? requestCounter.get() : 0;
            long pathRespTime = responseTimeCounter != null ? responseTimeCounter.get() : 0;
            long pathErrors = errorCounter != null ? errorCounter.get() : 0;
            
            stats.setTotalRequests(pathReqs);
            
            if (pathReqs > 0) {
                stats.setAvgResponseTime((double) pathRespTime / pathReqs);
                stats.setErrorRate(((double) pathErrors / pathReqs) * 100);
            } else {
                stats.setAvgResponseTime(0.0);
                stats.setErrorRate(0.0);
            }
            
            pathStats.add(stats);
        }
        metrics.setPathStats(pathStats);

        return metrics;
    }

    // 记录API调用
    public void recordApiCall(String path, long responseTime, int statusCode) {
        // 确保在访问计数器之前初始化它们
        pathTotalRequests.computeIfAbsent(path, k -> new AtomicLong(0)).incrementAndGet();
        pathTotalResponseTime.computeIfAbsent(path, k -> new AtomicLong(0)).addAndGet(responseTime);
        
        if (statusCode >= 400) {
            pathTotalErrors.computeIfAbsent(path, k -> new AtomicLong(0)).incrementAndGet();
        }

        ApiMetrics.ApiInfo apiInfo = new ApiMetrics.ApiInfo();
        apiInfo.setTimestamp(System.currentTimeMillis());
        apiInfo.setPath(path);
        apiInfo.setResponseTime(responseTime);
        apiInfo.setStatusCode(statusCode);

        // 更新历史记录
        LinkedList<ApiMetrics.ApiInfo> history = apiHistoryMap.computeIfAbsent(path, k -> new LinkedList<>());
        
        synchronized (history) {
            history.addFirst(apiInfo);
            while (history.size() > MAX_HISTORY_PER_PATH) {
                history.removeLast();
            }
        }

        // 如果路径数量超过限制，移除最旧的路径
        if (apiHistoryMap.size() > MAX_PATHS) {
            String oldestPath = findOldestPath();
            if (oldestPath != null) {
                apiHistoryMap.remove(oldestPath);
                pathTotalRequests.remove(oldestPath);
                pathTotalResponseTime.remove(oldestPath);
                pathTotalErrors.remove(oldestPath);
            }
        }
    }

    private String findOldestPath() {
        long oldestTimestamp = Long.MAX_VALUE;
        String oldestPath = null;
        for (Map.Entry<String, LinkedList<ApiMetrics.ApiInfo>> entry : apiHistoryMap.entrySet()) {
            LinkedList<ApiMetrics.ApiInfo> history = entry.getValue();
            if (!history.isEmpty()) {
                synchronized (history) {
                    ApiMetrics.ApiInfo lastCall = history.getLast();
                    if (lastCall.getTimestamp() < oldestTimestamp) {
                        oldestTimestamp = lastCall.getTimestamp();
                        oldestPath = entry.getKey();
                    }
                }
            }
        }
        return oldestPath;
    }
} 