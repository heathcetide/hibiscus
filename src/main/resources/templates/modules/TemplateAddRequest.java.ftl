package ${packageName}.generator.model.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 创建${dataName}请求
 *
 * @author Hibiscus Cetide
 * @from cetide
 */
public class ${upperDataKey}AddRequest implements Serializable {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;

    public String getTitle() {
    return title;
    }

    public void setTitle(String title) {
    this.title = title;
    }

    public String getContent() {
    return content;
    }

    public void setContent(String content) {
    this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}