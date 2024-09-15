package hibiscus.cetide.app.service;

import hibiscus.cetide.app.model.BaseUser;
import hibiscus.cetide.app.model.BaseUserQueryParam;
import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Page;

public interface BaseUserService {

  /**
   * 新增
   */
  BaseUser add(BaseUser user);

  /**
   * id 查询
   */
  BaseUser get(String id);

  /**
   * 条件查询，支持分页
   */
  Page<BaseUser> list(BaseUserQueryParam param);
  /**
   * 修改
   */
  boolean modify(BaseUser user);

  /**
   * 删除
   */
  boolean delete(String id);


}
