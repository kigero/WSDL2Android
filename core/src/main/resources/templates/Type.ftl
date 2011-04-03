package ${package};

<#if importBindings??>import ${importBindings}.*;</#if>
import org.xmlpull.v1.XmlSerializer;
import java.io.IOException;

public class ${typeName} extends SOAPObject
{
    <#if atts??>
    <#list atts as att>
    public ${att.javaType} ${att.name} = null;
    </#list>
    </#if>

    public String getNamespace()
    {
        return "<#if namespace??>${namespace}</#if>";
    }

    public void addAttributesToNode(XmlSerializer xml) throws IOException
    {
        <#if atts??>
        <#list atts as att>
        if(${att.name} != null)
        {
            xml.attribute(null, "${att.name}", ${att.name});
        }
        </#list>
        </#if>
    }

    public void addElementsToNode(XmlSerializer xml) throws IOException
    {

    }
}
