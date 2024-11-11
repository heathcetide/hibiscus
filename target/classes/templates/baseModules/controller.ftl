package ${packageName}.generate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ${packageName}.generate.service.${className}Service;
import ${packageName}.generate.model.${className};
/**
*
* @author Hibiscus Cetide
*
* @from cetide
*/
@RestController
@RequestMapping("/${className?uncap_first}")
public class ${className}Controller {

@Autowired
private ${className}Service ${className?uncap_first}Service;

@PostMapping
public void add(@RequestBody ${className} ${className?uncap_first}) {
${className?uncap_first}Service.save(${className?uncap_first});
}

@DeleteMapping("/{id}")
public void delete(@PathVariable Long id) {
${className?uncap_first}Service.removeById(id);
}

@PutMapping
public void update(@RequestBody ${className} ${className?uncap_first}) {
${className?uncap_first}Service.updateById(${className?uncap_first});
}

@GetMapping("/{id}")
public ${className} get(@PathVariable Long id) {
return ${className?uncap_first}Service.getById(id);
}
}
