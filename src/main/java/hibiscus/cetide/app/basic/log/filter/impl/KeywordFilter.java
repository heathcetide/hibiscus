package hibiscus.cetide.app.basic.log.filter.impl;

import hibiscus.cetide.app.basic.log.filter.LogFilter;

public class KeywordFilter implements LogFilter {
    private String keyword;

    public KeywordFilter(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public boolean accept(String logLine) {
        return logLine.contains(keyword);
    }
}
