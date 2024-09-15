package hibiscus.cetide.app.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import hibiscus.cetide.app.model.BaseUser;
import hibiscus.cetide.app.model.BaseUserQueryParam;
import hibiscus.cetide.app.service.BaseUserService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import hibiscus.cetide.app.utils.ExpiredLRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BaseUserServiceImpl implements BaseUserService {
  @Autowired
  private ExpiredLRUCache<String, Object> cache;
  private static final Logger LOG = LoggerFactory.getLogger(BaseUserServiceImpl.class);

  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public BaseUser add(BaseUser user) {
    // 作为服务，要对入参进行判断，不能假设被调用时，传入的一定是真正的对象
    if (user == null) {
      LOG.error("input User data is null.");
      return null;
    }
    cache.put(user.getLoginName(), String.valueOf(user));
    System.out.println(cache.get(user.getId()));
    return null;
//    return mongoTemplate.insert(user);
  }

  @Override
  public BaseUser get(String id) {
    // 输入的主键 id 必须有文本，不能为空或全空格
    if (!StringUtils.hasText(id)) {
      LOG.error("input user Id is blank.");
      return null;
    }

    BaseUser user = mongoTemplate.findById(id, BaseUser.class);
    return user;
  }

  @Override
  public Page<BaseUser> list(BaseUserQueryParam param) {
    // 作为服务，要对入参进行判断，不能假设被调用时，入参一定正确
    if (param == null) {
      LOG.error("input User data is not correct.");
      return null;
    }

    // 总条件
    Criteria criteria = new Criteria();
    // 可能有多个子条件
    List<Criteria> subCris = new ArrayList();
    if (StringUtils.hasText(param.getLoginName())) {
      subCris.add(Criteria.where("loginName").is(param.getLoginName()));
    }

    if (StringUtils.hasText(param.getPassword())) {
      subCris.add(Criteria.where("password").is(param.getPassword()));
    }

    if (StringUtils.hasText(param.getMobile())) {
      subCris.add(Criteria.where("mobile").is(param.getMobile()));
    }

    // 必须至少有一个查询条件
    if (subCris.isEmpty()) {
      LOG.error("input User query param is not correct.");
      return null;
    }

    // 三个子条件以 and 关键词连接成总条件对象，相当于 name='' and lyrics='' and subjectId=''
    criteria.andOperator(subCris.toArray(new Criteria[] {}));

    // 条件对象构建查询对象
    Query query = new Query(criteria);
    // 总数
    long count = mongoTemplate.count(query, BaseUser.class);
    // 构建分页对象。注意此对象页码号是从 0 开始计数的。
    Pageable pageable = PageRequest.of(param.getPageNum() - 1, param.getPageSize());
    query.with(pageable);

    // 查询结果
    List<BaseUser> users = mongoTemplate.find(query, BaseUser.class);
    // 构建分页器
    Page<BaseUser> pageResult = PageableExecutionUtils.getPage(users, pageable, new LongSupplier() {
      @Override
      public long getAsLong() {
        return count;
      }
    });

    return pageResult;
  }

  @Override
  public boolean modify(BaseUser user) {
    // 作为服务，要对入参进行判断，不能假设被调用时，入参一定正确
    if (user == null || !StringUtils.hasText(user.getId())) {
      LOG.error("input User data is not correct.");
      return false;
    }

    // 主键不能修改，作为查询条件
    Query query = new Query(Criteria.where("id").is(user.getId()));

    Update updateData = new Update();
    // 值为 null 表示不修改。值为长度为 0 的字符串 "" 表示清空此字段
    if (user.getLoginName() != null) {
      updateData.set("loginName", user.getLoginName());
    }

    if (user.getPassword() != null) {
      updateData.set("password", user.getPassword());
    }

    if (user.getMobile() != null) {
      updateData.set("mobile", user.getMobile());
    }

    // 把一条符合条件的记录，修改其字段
    UpdateResult result = mongoTemplate.updateFirst(query, updateData, BaseUser.class);
    return result != null && result.getModifiedCount() > 0;
  }

  @Override
  public boolean delete(String id) {
    // 输入的主键 id 必须有文本，不能为空或全空格
    if (!StringUtils.hasText(id)) {
      LOG.error("input user Id is blank.");
      return false;
    }

    BaseUser user = new BaseUser();
    user.setId(id);

    DeleteResult result = mongoTemplate.remove(user);
    return result != null && result.getDeletedCount() > 0;
  }
}
