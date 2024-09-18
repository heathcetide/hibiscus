package hibiscus.cetide.app.service.impl;

import hibiscus.cetide.app.core.model.BaseUser;
import hibiscus.cetide.app.service.BaseUserService;
import hibiscus.cetide.app.utils.ExpiredLRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BaseUserServiceImpl implements BaseUserService {
  @Autowired
  private ExpiredLRUCache<String, Object> cache;
  private static final Logger LOG = LoggerFactory.getLogger(BaseUserServiceImpl.class);

  @Override
  public BaseUser add(BaseUser user) {
    // 作为服务，要对入参进行判断，不能假设被调用时，传入的一定是真正的对象
    if (user == null) {
      LOG.error("input User data is null.");
      return null;
    }
    cache.put(user.getLoginName(), user);
    return new BaseUser(user.getId(), user.getLoginName(),user.getPassword(), user.getPassword());
  }
}
