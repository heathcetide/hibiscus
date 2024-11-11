package ${packageName}.generate.model;

/**
*
* @author Hibiscus Cetide
*
* @from cetide
*/
public class ${className} {

<#list fields as field>
    private ${field.type} ${field.name};
</#list>

// 无参构造方法
public ${className}() {}

// 有参构造方法
public ${className}(
<#list fields as field>
    ${field.type} ${field.name}<#if field_has_next>,</#if>
</#list>
) {
<#list fields as field>
    this.${field.name} = ${field.name};
</#list>
}

// Getter 和 Setter 方法
<#list fields as field>
    public ${field.type} get${field.name?cap_first}() {
    return ${field.name};
    }

    public void set${field.name?cap_first}(${field.type} ${field.name}) {
    this.${field.name} = ${field.name};
    }
</#list>

@Override
public String toString() {
return "${className}{" +
<#list fields as field>
    "${field.name}=" + ${field.name}<#if field_has_next> + ", " + </#if>
</#list>
+ '}';
}
}
