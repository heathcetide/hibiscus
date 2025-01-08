package hibiscus.cetide.app.component.cache.analytics;

import java.util.Map;

public class OptimizationRecommendation {
    private final OptimizationType type;
    private final String description;
    private final Map<String, Object> parameters;
    
    public OptimizationRecommendation(OptimizationType type, String description, Map<String, Object> parameters) {
        this.type = type;
        this.description = description;
        this.parameters = parameters;
    }
    
    public OptimizationType getType() {
        return type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
} 