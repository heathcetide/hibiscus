# API Documentation

## Class: ${className}
${classDescription}

<#list methods as method>
    ### Method: ${method.name}
    **Description**: ${method.description}
    **Return Type**: ${method.returnType}

    #### Parameters:
    <#list method.parameters as param>
        - **${param.name}** (${param.type}): ${param.description}
    </#list>
</#list>
