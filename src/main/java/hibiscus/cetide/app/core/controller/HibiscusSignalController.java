package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.core.model.SignalConfigDTO;
import hibiscus.cetide.app.core.model.SignalGroupDTO;
import hibiscus.cetide.app.core.model.SignalGroupInfoDTO;
import hibiscus.cetide.app.component.signal.DefaultSignalManager;
import hibiscus.cetide.app.component.signal.SignalConfig;
import hibiscus.cetide.app.component.signal.SignalManager;
import hibiscus.cetide.app.component.signal.SignalPriority;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/hibiscus/signals")
public class HibiscusSignalController {
    private final SignalManager signalManager;

    public HibiscusSignalController() {
        this.signalManager = new DefaultSignalManager();
        this.signalManager.initialize();
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Map<String, Object>>> getAllMetrics() {
        return ResponseEntity.ok(signalManager.getAllMetrics());
    }

    @GetMapping("/metrics/{signalName}")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable String signalName) {
        return ResponseEntity.ok(signalManager.getMetrics(signalName));
    }

    @PostMapping("/{signalName}/config")
    public ResponseEntity<Void> updateConfig(
            @PathVariable String signalName,
            @RequestBody SignalConfigDTO configDTO) {
        SignalConfig config = new SignalConfig.Builder()
            .async(configDTO.isAsync())
            .maxRetries(configDTO.getMaxRetries())
            .retryDelayMs(configDTO.getRetryDelayMs())
            .maxHandlers(configDTO.getMaxHandlers())
            .timeoutMs(configDTO.getTimeoutMs())
            .recordMetrics(configDTO.isRecordMetrics())
            .priority(configDTO.getPriority())
            .groupName(configDTO.getGroupName())
            .persistent(configDTO.isPersistent())
            .build();

        signalManager.updateConfig(signalName, config);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{signalName}")
    public ResponseEntity<Void> clearSignal(@PathVariable String signalName) {
        signalManager.clear(signalName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/groups")
    public ResponseEntity<Void> createGroup(@RequestBody SignalGroupDTO groupDTO) {
        if (groupDTO.getDefaultConfig() != null) {
            signalManager.createGroup(groupDTO.getName(), convertToConfig(groupDTO.getDefaultConfig()));
        } else {
            signalManager.createGroup(groupDTO.getName());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/groups")
    public ResponseEntity<List<SignalGroupInfoDTO>> getAllGroups() {
        List<String> groupNames = signalManager.getAllGroups();
        List<SignalGroupInfoDTO> groupInfos = new ArrayList<>();
        
        for (String groupName : groupNames) {
            Set<String> signals = signalManager.getGroupSignals(groupName);
            groupInfos.add(new SignalGroupInfoDTO(groupName, signals.size()));
        }
        return ResponseEntity.ok(groupInfos);
    }

    @PostMapping("/groups/{groupName}/signals/{signalName}")
    public ResponseEntity<Void> addToGroup(
            @PathVariable String groupName,
            @PathVariable String signalName) {
        signalManager.addToGroup(groupName, signalName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/groups/{groupName}/signals/{signalName}")
    public ResponseEntity<Void> removeFromGroup(
            @PathVariable String groupName,
            @PathVariable String signalName) {
        signalManager.removeFromGroup(groupName, signalName);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/groups/{groupName}/signals")
    public ResponseEntity<Set<String>> getGroupSignals(@PathVariable String groupName) {
        return ResponseEntity.ok(signalManager.getGroupSignals(groupName));
    }

    @DeleteMapping("/groups/{groupName}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String groupName) {
        signalManager.deleteGroup(groupName);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{signalName}/priority")
    public ResponseEntity<Void> setPriority(
            @PathVariable String signalName,
            @RequestBody SignalPriority priority) {
        signalManager.setPriority(signalName, priority);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{signalName}/priority")
    public ResponseEntity<SignalPriority> getPriority(@PathVariable String signalName) {
        return ResponseEntity.ok(signalManager.getPriority(signalName));
    }

    @PostMapping("/{signalName}/metrics/enable")
    public ResponseEntity<Void> enableMetrics(@PathVariable String signalName) {
        signalManager.enableMetrics(signalName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{signalName}/metrics/disable")
    public ResponseEntity<Void> disableMetrics(@PathVariable String signalName) {
        signalManager.disableMetrics(signalName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{signalName}/persistence/enable")
    public ResponseEntity<Void> enablePersistence(@PathVariable String signalName) {
        signalManager.enablePersistence(signalName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{signalName}/persistence/disable")
    public ResponseEntity<Void> disablePersistence(@PathVariable String signalName) {
        signalManager.disablePersistence(signalName);
        return ResponseEntity.ok().build();
    }

    private SignalConfig convertToConfig(SignalConfigDTO dto) {
        return new SignalConfig.Builder()
            .async(dto.isAsync())
            .maxRetries(dto.getMaxRetries())
            .retryDelayMs(dto.getRetryDelayMs())
            .maxHandlers(dto.getMaxHandlers())
            .timeoutMs(dto.getTimeoutMs())
            .recordMetrics(dto.isRecordMetrics())
            .priority(dto.getPriority())
            .groupName(dto.getGroupName())
            .persistent(dto.isPersistent())
            .build();
    }
}


