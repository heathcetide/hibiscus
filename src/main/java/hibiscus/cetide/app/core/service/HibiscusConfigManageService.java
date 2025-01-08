package hibiscus.cetide.app.core.service;

import hibiscus.cetide.app.core.model.ConfigHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HibiscusConfigManageService {
    private static final Logger log = LoggerFactory.getLogger(HibiscusConfigManageService.class);
    private static final String BACKUP_DIR = "config-backup";
    private static final String HISTORY_FILE = BACKUP_DIR + "/history.json";
    private List<ConfigHistory> historyList = new ArrayList<>();
    private final ObjectMapper objectMapper;
    
    public HibiscusConfigManageService() {
        this.objectMapper = new ObjectMapper();
        // 配置ObjectMapper以处理Java 8日期时间
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 创建备份目录
        try {
            Files.createDirectories(Paths.get(BACKUP_DIR));
            loadHistory();
        } catch (IOException e) {
            log.error("初始化配置管理服务失败", e);
        }
    }

    /**
     * 备份配置文件
     */
    public ConfigHistory backupConfig(String filePath, String operator, String description) throws IOException {
        // 获取项目根目录
        String rootPath = System.getProperty("user.dir");
        
        // 处理相对路径
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        
        Path sourcePath = Paths.get(rootPath, filePath);
        if (!Files.exists(sourcePath)) {
            throw new FileNotFoundException("配置文件不存在：" + filePath);
        }

        // 创建备份目录（如果不存在）
        Files.createDirectories(Paths.get(BACKUP_DIR));

        // 创建备份文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = sourcePath.getFileName().toString();
        String backupFileName = fileName + "." + timestamp + ".bak";
        Path backupPath = Paths.get(BACKUP_DIR, backupFileName);

        // 复制文件
        Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);

        // 创建历史记录
        ConfigHistory history = new ConfigHistory();
        history.setId((long) (historyList.size() + 1));
        history.setFileName(fileName);
        history.setFilePath(filePath);
        history.setBackupPath(backupPath.toString());
        history.setOperator(operator);
        history.setDescription(description);
        history.setCreateTime(LocalDateTime.now());
        history.setActive(true);

        // 更新历史记录
        updateHistoryStatus(filePath, false); // 将之前的记录标记为非活动
        historyList.add(history);
        saveHistory();

        return history;
    }

    /**
     * 回滚配置文件
     */
    public void rollbackConfig(Long historyId) throws IOException {
        ConfigHistory history = findHistoryById(historyId);
        if (history == null) {
            throw new IllegalArgumentException("找不到历史记录：" + historyId);
        }

        // 获取项目根目录
        String rootPath = System.getProperty("user.dir");
        
        // 复制备份文件到原始位置
        Path backupPath = Paths.get(history.getBackupPath());
        Path originalPath = Paths.get(rootPath, history.getFilePath());
        
        if (!Files.exists(backupPath)) {
            throw new FileNotFoundException("备份文件不存在：" + history.getBackupPath());
        }
        
        // 先创建当前配置的备份
        backupConfig(history.getFilePath(), "SYSTEM", "回滚前自动备份");
        
        // 执行回滚
        Files.copy(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING);

        // 更新历史记录状态
        updateHistoryStatus(history.getFilePath(), false);
        history.setActive(true);
        saveHistory();
        
        log.info("配置文件已回滚到版本 {}: {}", historyId, history.getDescription());
    }

    /**
     * 获取配置文件的修改历史
     */
    public List<ConfigHistory> getHistory(String filePath) {
        return historyList.stream()
                .filter(h -> h.getFilePath().equals(filePath))
                .sorted((h1, h2) -> h2.getCreateTime().compareTo(h1.getCreateTime()))
                .collect(Collectors.toList());
    }

    /**
     * 验证配置文件
     */
    public boolean validateConfig(String content) {
        try {
            Yaml yaml = new Yaml();
            yaml.load(content);
            return true;
        } catch (Exception e) {
            log.error("配置文件验证失败", e);
            return false;
        }
    }

    /**
     * 比较两个配置文件的差异
     */
    public Map<String, Object> compareConfigs(Long historyId1, Long historyId2) throws IOException {
        ConfigHistory history1 = findHistoryById(historyId1);
        ConfigHistory history2 = findHistoryById(historyId2);
        
        if (history1 == null || history2 == null) {
            throw new IllegalArgumentException("找不到历史记录");
        }

        // 读取备份文件内容
        String content1 = "";
        String content2 = "";
        
        try {
            content1 = new String(Files.readAllBytes(Paths.get(history1.getBackupPath())), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取备份文件失败: " + history1.getBackupPath(), e);
            throw new IOException("读取备份文件失败: " + history1.getBackupPath());
        }
        
        try {
            content2 = new String(Files.readAllBytes(Paths.get(history2.getBackupPath())), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取备份文件失败: " + history2.getBackupPath(), e);
            throw new IOException("读取备份文件失败: " + history2.getBackupPath());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("history1", history1);
        result.put("history2", history2);
        result.put("content1", content1);
        result.put("content2", content2);
        
        return result;
    }

    /**
     * 获取配置文件内容
     */
    public String getConfigContent(String filePath) throws IOException {
        // 处理相对路径，确保从项目根目录开始
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        
        // 获取项目根目录
        String rootPath = System.getProperty("user.dir");
        Path path = Paths.get(rootPath, filePath);
        
        if (!Files.exists(path)) {
            throw new FileNotFoundException("配置文件不存在：" + filePath);
        }

        // 读取文件内容
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * 保存配置文件内容
     */
    public ConfigHistory saveConfig(String filePath, String content, String operator, String description) throws IOException {
        // 获取项目根目录
        String rootPath = System.getProperty("user.dir");
        
        // 处理相对路径
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        
        // 先创建备份
        ConfigHistory history = backupConfig(filePath, operator, description);
        
        // 更新原始文件内容
        Path path = Paths.get(rootPath, filePath);
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        
        return history;
    }

    private void loadHistory() {
        Path historyPath = Paths.get(HISTORY_FILE);
        if (Files.exists(historyPath)) {
            try {
                String content = new String(Files.readAllBytes(historyPath));
                if (!content.trim().isEmpty()) {
                    historyList = objectMapper.readValue(content, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ConfigHistory.class));
                }
            } catch (IOException e) {
                log.error("加载历史记录失败", e);
            }
        }
    }

    private void saveHistory() {
        try {
            String json = objectMapper.writeValueAsString(historyList);
            Files.write(Paths.get(HISTORY_FILE), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("保存历史记录失败", e);
        }
    }

    private void updateHistoryStatus(String filePath, boolean active) {
        historyList.stream()
                .filter(h -> h.getFilePath().equals(filePath))
                .forEach(h -> h.setActive(active));
    }

    private ConfigHistory findHistoryById(Long id) {
        return historyList.stream()
                .filter(h -> h.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取当前活动版本ID
     */
    public Long getCurrentVersionId(String filePath) {
        return historyList.stream()
                .filter(h -> h.getFilePath().equals(filePath) && h.isActive())
                .map(ConfigHistory::getId)
                .findFirst()
                .orElse(1L);
    }
} 