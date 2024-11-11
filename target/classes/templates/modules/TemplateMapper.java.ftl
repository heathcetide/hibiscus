package ${packageName}.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import hibiscus.cetide.app.generator.model.entity.${upperDataKey};
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ${upperDataKey}Mapper extends BaseMapper<${upperDataKey}> {
}
