package hibiscus.cetide.app.component.signal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SignalPersistence {
    private static final Logger logger = LoggerFactory.getLogger(SignalPersistence.class);
    private static final String PERSISTENCE_DIR = "signal_data";
    private static final String CONFIG_FILE = "signal_config.json";
    private static final String METRICS_FILE = "signal_metrics.json";
    private final ObjectMapper objectMapper;
    private final Path basePath;

    public SignalPersistence() {
        this.objectMapper = new ObjectMapper();
        this.basePath = Paths.get(PERSISTENCE_DIR);
        initializeDirectory();
    }

    private void initializeDirectory() {
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            logger.error("Failed to create persistence directory", e);
        }
    }

    public void saveSignalConfig(String signalName, SignalConfig config) {
        if (!config.isPersistent()) {
            return;
        }

        try {
            Map<String, Object> configMap = new ConcurrentHashMap<>();
            configMap.put("async", config.isAsync());
            configMap.put("maxRetries", config.getMaxRetries());
            configMap.put("retryDelayMs", config.getRetryDelayMs());
            configMap.put("maxHandlers", config.getMaxHandlers());
            configMap.put("timeoutMs", config.getTimeoutMs());
            configMap.put("recordMetrics", config.isRecordMetrics());
            configMap.put("priority", config.getPriority().name());
            configMap.put("groupName", config.getGroupName());
            configMap.put("persistent", config.isPersistent());

            String json = objectMapper.writeValueAsString(configMap);
            Path configPath = basePath.resolve(signalName + "_" + CONFIG_FILE);
            Files.write(configPath, json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to save signal config for " + signalName, e);
        }
    }

    public SignalConfig loadSignalConfig(String signalName) {
        try {
            Path configPath = basePath.resolve(signalName + "_" + CONFIG_FILE);
            if (!Files.exists(configPath)) {
                return null;
            }

            String json = new String(Files.readAllBytes(configPath));
            Map<String, Object> configMap = objectMapper.readValue(json, Map.class);

            return new SignalConfig.Builder()
                .async((Boolean) configMap.get("async"))
                .maxRetries((Integer) configMap.get("maxRetries"))
                .retryDelayMs((Long) configMap.get("retryDelayMs"))
                .maxHandlers((Integer) configMap.get("maxHandlers"))
                .timeoutMs((Long) configMap.get("timeoutMs"))
                .recordMetrics((Boolean) configMap.get("recordMetrics"))
                .priority(SignalPriority.valueOf((String) configMap.get("priority")))
                .groupName((String) configMap.get("groupName"))
                .persistent((Boolean) configMap.get("persistent"))
                .build();
        } catch (IOException e) {
            logger.error("Failed to load signal config for " + signalName, e);
            return null;
        }
    }

    public void saveSignalMetrics(String signalName, Map<String, Object> metrics) {
        try {
            String json = objectMapper.writeValueAsString(metrics);
            Path metricsPath = basePath.resolve(signalName + "_" + METRICS_FILE);
            Files.write(metricsPath, json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to save signal metrics for " + signalName, e);
        }
    }

    public Map<String, Object> loadSignalMetrics(String signalName) {
        try {
            Path metricsPath = basePath.resolve(signalName + "_" + METRICS_FILE);
            if (!Files.exists(metricsPath)) {
                return null;
            }

            String json = new String(Files.readAllBytes(metricsPath));
            return objectMapper.readValue(json, Map.class);
        } catch (IOException e) {
            logger.error("Failed to load signal metrics for " + signalName, e);
            return null;
        }
    }

    public void deleteSignalData(String signalName) {
        try {
            Files.deleteIfExists(basePath.resolve(signalName + "_" + CONFIG_FILE));
            Files.deleteIfExists(basePath.resolve(signalName + "_" + METRICS_FILE));
        } catch (IOException e) {
            logger.error("Failed to delete signal data for " + signalName, e);
        }
    }
} 