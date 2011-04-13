package ${package};

<#if importBindings??>import ${importBindings}.*;</#if>
import org.xmlpull.v1.XmlSerializer;
import java.io.IOException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ${typeName} extends SOAPObject 
{
    <#if atts??>
    <#list atts as att>
    public ${att.javaType} ${att.name} = null;
    </#list>
    </#if>

    <#if elements??>
    <#list elements as element>
    public ${element.javaType} ${element.name} = null;
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
            xml.attribute(null, "${att.name}", ${att.name}.toString());
        }
        </#list>
        </#if>
    }

    public void addElementsToNode(XmlSerializer xml) throws IOException
    {
        <#if elements??>
        <#list elements as element>
        if(${element.name} != null)
        {
            xml.attribute(null, "${element.name}", ${element.name}.toString());
        }
        </#list>
        </#if>
    }

    public void parse(SOAPBinding binding, Element el)
    {
        <#if atts??>
        //attributes
        <#list atts as att>
        ${att.name} = (${att.javaType}) parseAttribute(binding, el.getAttribute("${att.name}"), this, "${att.name}");
        </#list>
        </#if>

        <#if elements??>
        NodeList children = el.getChildNodes();
        for(int x = 0;x < children.getLength();x++)
        {
            Node childNde = children.item(x);
            if(childNde.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }

            Element childEl = (Element) childNde;
            <#list elements as element>
            if(childEl.getLocalName().equals("${element.name}"))
            {
                ${element.name} = (${element.javaType}) parseElement(binding, childEl, this, "${element.name}"); 
            }
            </#list>
        }
        </#if>
    }
}
