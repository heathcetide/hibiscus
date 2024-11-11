package ${packageName}.generator.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ${packageName}.generator.common.ErrorCode;
import ${packageName}.generator.common.CommonConstant;
import ${packageName}.generator.common.ThrowUtils;
import ${packageName}.generator.mapper.${upperDataKey}Mapper;
import ${packageName}.generator.model.dto.${upperDataKey}QueryRequest;
import ${packageName}.generator.model.entity.${upperDataKey};
import ${packageName}.generator.model.entity.User;
import ${packageName}.generator.model.vo.${upperDataKey}VO;
import ${packageName}.generator.model.vo.UserVO;
import ${packageName}.generator.service.${upperDataKey}Service;
import ${packageName}.generator.service.UserService;
import ${packageName}.generator.common.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ${dataName}服务实现
 *
 * @author Hibiscus Cetide
 */
@Service
public class ${upperDataKey}ServiceImpl extends ServiceImpl<${upperDataKey}Mapper, ${upperDataKey}> implements ${upperDataKey}Service {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param ${dataKey}
     * @param add      对创建的数据进行校验
     */
    @Override
    public void valid${upperDataKey}(${upperDataKey} ${dataKey}, boolean add) {
        ThrowUtils.throwIf(${dataKey} == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = ${dataKey}.getTitle();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param ${dataKey}QueryRequest
     * @return
     */
    @Override
    public QueryWrapper<${upperDataKey}> getQueryWrapper(${upperDataKey}QueryRequest ${dataKey}QueryRequest) {
        QueryWrapper<${upperDataKey}> queryWrapper = new QueryWrapper<>();
        if (${dataKey}QueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = ${dataKey}QueryRequest.getId();
        Long notId = ${dataKey}QueryRequest.getNotId();
        String title = ${dataKey}QueryRequest.getTitle();
        String content = ${dataKey}QueryRequest.getContent();
        String searchText = ${dataKey}QueryRequest.getSearchText();
        String sortField = ${dataKey}QueryRequest.getSortField();
        String sortOrder = ${dataKey}QueryRequest.getSortOrder();
        List<String> tagList = ${dataKey}QueryRequest.getTags();
        Long userId = ${dataKey}QueryRequest.getUserId();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
