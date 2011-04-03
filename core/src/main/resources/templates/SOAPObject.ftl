package ${package};

import org.xmlpull.v1.XmlSerializer;
import java.io.IOException;

public abstract class SOAPObject
{
    public void toXml(XmlSerializer xml, String name, String namespace) throws IOException
    {
        String ns = null;
        if(namespace != null && namespace.length() > 0)
        {
            ns = namespace;
        }
        else
        {
            ns = getNamespace();
        }

        xml.startTag(ns, name);
        addAttributesToNode(xml);

        xml.endTag(ns, name);
    }

    public abstract String getNamespace();
    public abstract void addAttributesToNode(XmlSerializer xml) throws IOException;
    public abstract void addElementsToNode(XmlSerializer xml) throws IOException;
}
