package hibiscus.cetide.app.component.cache.analytics;

public class AnalyticsReport {
    private int hotKeyCount;
    private int totalPatterns;
    private double averageScore;
    
    public void setHotKeyCount(int count) {
        this.hotKeyCount = count;
    }
    
    public void setTotalPatterns(int count) {
        this.totalPatterns = count;
    }
    
    public void setAverageScore(double score) {
        this.averageScore = score;
    }
    
    @Override
    public String toString() {
        return String.format(
            "HotKeys: %d, TotalPatterns: %d, AvgScore: %.2f",
            hotKeyCount, totalPatterns, averageScore
        );
    }
} 