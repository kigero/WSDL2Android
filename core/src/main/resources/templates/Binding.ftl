package ${package};

<#if importTypes??>import ${importTypes}.*;</#if>
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

public class ${bindingName} extends SOAPBinding
{
    public ${bindingName}(String endpoint)
    {
        super(endpoint);
    }
    
    public Map<String, String> getNamespaces()
    {
        Map<String, String> namespaces = new HashMap<String, String>();
        <#list namespaces as namespace>
        namespaces.put("${namespace.prefix}", "${namespace.uri}");
        </#list>

        return namespaces;
    }

    <#list bOps as bOp>
    public ${bOp.return} ${bOp.name}(${bOp.paramString}) throws IOException
    {
        Map<String, SOAPObject> bodyElements = new HashMap<String, SOAPObject>();
        <#list bOp.params as param>
        if(${param.name} != null)
        {
            bodyElements.put("${param.type}", ${param.name});
        }
        </#list>

        <#if bOp.return = "void">
        makeRequest(bodyElements);
        <#else>
        SOAPEnvelope env = makeRequest(bodyElements);  
        ${bOp.return} rtrn = null;
        for(Object o : env.bodyElements)
        {
            if(o != null && o instanceof ${bOp.return})
            {
                rtrn = (${bOp.return}) o;
                break;
            }
        }

        return rtrn;
        </#if>
    }

    </#list>
}
