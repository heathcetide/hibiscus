package ${packageName}.generator.model.vo;

import ${packageName}.generator.model.entity.${upperDataKey};
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * ${dataName}视图
 *
 * @author Hibiscus Cetide
 * @from cetide
 */
public class ${upperDataKey}VO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 创建用户信息
     */
    private UserVO user;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<String> getTagList() {
        return tagList;
   }

   public void setTagList(List<String> tagList) {
       this.tagList = tagList;
   }

   public UserVO getUser() {
       return user;
   }

   public void setUser(UserVO user) {
       this.user = user;
   }


   /**
   * 封装类转对象
   *
   * @param ${dataKey}VO
   * @return
   */
   public static ${upperDataKey} voToObj(${upperDataKey}VO ${dataKey}VO) {
   if (${dataKey}VO == null) {
        return null;
   }
   ${upperDataKey} ${dataKey} = new ${upperDataKey}();
   BeanUtils.copyProperties(${dataKey}VO, ${dataKey});
   return ${dataKey};
   }

   /**
    * 对象转封装类
    *
    * @param ${dataKey}
    * @return
   */
   public static ${upperDataKey}VO objToVo(${upperDataKey} ${dataKey}) {
       if (${dataKey} == null) {
          return null;
       }
       ${upperDataKey}VO ${dataKey}VO = new ${upperDataKey}VO();
       BeanUtils.copyProperties(${dataKey}, ${dataKey}VO);
       return ${dataKey}VO;
   }
}
