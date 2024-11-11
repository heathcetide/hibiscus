package ${packageName}.generator.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import ${packageName}.generator.model.dto.${upperDataKey}QueryRequest;
import ${packageName}.generator.model.entity.${upperDataKey};
import ${packageName}.generator.model.vo.${upperDataKey}VO;

import javax.servlet.http.HttpServletRequest;

/**
* ${dataName}服务
*
* @author Hibiscus Cetide
*/
public interface ${upperDataKey}Service extends IService<${upperDataKey}> {

    /**
     * 校验数据
     *
     * @param ${dataKey}
     * @param add 对创建的数据进行校验
     */
    void valid${upperDataKey}(${upperDataKey} ${dataKey}, boolean add);

    /**
     * 获取查询条件
     *
     * @param ${dataKey}QueryRequest
     * @return
     */
    QueryWrapper<${upperDataKey}> getQueryWrapper(${upperDataKey}QueryRequest ${dataKey}QueryRequest);

}
