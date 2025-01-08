package hibiscus.cetide.app.component.cache.analytics;

import java.util.List;

public class OptimizationResult {
    private final List<OptimizationRecommendation> recommendations;
    
    public OptimizationResult(List<OptimizationRecommendation> recommendations) {
        this.recommendations = recommendations;
    }
    
    public List<OptimizationRecommendation> getRecommendations() {
        return recommendations;
    }
} 