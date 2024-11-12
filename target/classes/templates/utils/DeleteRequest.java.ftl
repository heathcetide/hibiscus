package ${packageName}.generator.common;

import java.io.Serializable;

/**
 * 删除请求
 *
 * @author Hibiscus Cetide
 */
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}