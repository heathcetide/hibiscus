package ${packageName}.generate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ${packageName}.generate.model.${className};
import java.util.List;

/**
*
* @author Hibiscus Cetide
*
* @from cetide
*/
public interface ${className}Service extends IService<${className}> {

    boolean save(${className} ${className?uncap_first});

    void removeById(Long id);

    boolean updateById(${className} ${className?uncap_first});

    ${className} getById(Long id);

    List<${className}> listAll();
}
