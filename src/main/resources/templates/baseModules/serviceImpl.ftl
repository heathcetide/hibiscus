package ${packageName}.generate.service.impl;

import ${packageName}.generate.service.${className}Service;
import ${packageName}.generate.model.${className};
import ${packageName}.generate.mapper.${className}Mapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ${className}ServiceImpl extends ServiceImpl<${className}Mapper, ${className}> implements ${className}Service {

    @Autowired
    private ${className}Mapper ${className?uncap_first}Mapper;

    @Override
    public boolean save(${className} ${className?uncap_first}) {
        ${className?uncap_first}Mapper.insert(${className?uncap_first});
        return false;
    }

    @Override
    public void removeById(Long id) {
        ${className?uncap_first}Mapper.deleteById(id);
    }

    @Override
    public boolean updateById(${className} ${className?uncap_first}) {
        ${className?uncap_first}Mapper.updateById(${className?uncap_first});
        return false;
    }

    @Override
    public ${className} getById(Long id) {
        return ${className?uncap_first}Mapper.selectById(id);
    }

    @Override
    public List<${className}> listAll() {
        return ${className?uncap_first}Mapper.selectList(null);
    }
}
