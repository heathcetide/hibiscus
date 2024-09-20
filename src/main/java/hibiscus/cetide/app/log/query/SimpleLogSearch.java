package hibiscus.cetide.app.log.query;

import java.util.ArrayList;
import java.util.List;

public class SimpleLogSearch implements LogSearch {
    private List<String> logLines;

    public SimpleLogSearch(List<String> logLines) {
        this.logLines = logLines;
    }

    @Override
    public List<String> search(String query) {
        List<String> results = new ArrayList<>();
        for (String logLine : logLines) {
            if (logLine.contains(query)) {
                results.add(logLine);
            }
        }
        return results;
    }
}