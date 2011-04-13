package ${package};

import java.util.Map;
import java.util.HashMap;
import java.io.StringWriter;
import org.xmlpull.v1.XmlSerializer;
import android.util.Xml;
import java.io.IOException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.List;
import java.util.ArrayList;

public class SOAPEnvelope extends SOAPObject
{
    public static final String NS_SOAP_ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String NS_XSL_TRANSFORM = "http://www.w3.org/1999/XSL/Transform";
    private static final String NS_XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String NS_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static final String TAG_ENVELOPE = "Envelope";
    private static final String TAG_BODY = "Body";
    private static final String TAG_HEADER = "Header";

    public List<Object> bodyElements = new ArrayList<Object>();

    public static String getXML(Map<String, String> namespaces, 
            Map<String, SOAPObject> headerElements, 
            Map<String, SOAPObject> bodyElements) throws IOException
    {
        StringWriter sw = new StringWriter();

        try
        {
            //Build the XML envelope
            XmlSerializer xml = Xml.newSerializer();
            xml.setOutput(sw);
            xml.startDocument("UTF-8", Boolean.valueOf(true));

            xml.setPrefix("soap", NS_SOAP_ENVELOPE);
            xml.setPrefix("xsl", NS_XSL_TRANSFORM);
            xml.setPrefix("xsi", NS_XML_SCHEMA_INSTANCE);
            xml.setPrefix("xsd", NS_XML_SCHEMA);

            xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xml.startTag(NS_SOAP_ENVELOPE, TAG_ENVELOPE);

            xml.startTag(NS_SOAP_ENVELOPE, TAG_HEADER);
            if(headerElements != null)
            {
                //TODO Add header elements
            }
            xml.endTag(NS_SOAP_ENVELOPE, TAG_HEADER);

            xml.startTag(NS_SOAP_ENVELOPE, TAG_BODY);

            if(bodyElements != null)
            {
                //Add body elements
                for(String key : bodyElements.keySet())
                {
                    SOAPObject bodyEl = bodyElements.get(key);

                    if(namespaces != null)
                    {
                        for(String prefix : namespaces.keySet())
                        {
                            xml.setPrefix(prefix, namespaces.get(prefix));
                        }
                    }

                    bodyEl.toXml(xml, key, null);
                }
            }

            xml.endTag(NS_SOAP_ENVELOPE, TAG_BODY);

            xml.endTag(NS_SOAP_ENVELOPE, TAG_ENVELOPE);
            xml.endDocument();
            xml.flush();

            return sw.toString();
        }
        finally
        {
            sw.close();
        }
    }

    public void parse(SOAPBinding binding, Element el)
    {
        NodeList children = el.getChildNodes();
        Element bodyEl = null;
        for(int x = 0;x < children.getLength();x++)
        {
            Element childEl = (Element) children.item(x);
            if(childEl.getLocalName().equals(TAG_BODY))
            {
                bodyEl = childEl;
                break;
            }
        }

        if(bodyEl == null)
        {
            //TODO: Do something...
            return;
        }

        //For each child of the body, parse it and add it to the children list.
        children = bodyEl.getChildNodes();
        for(int x = 0;x < children.getLength();x++)
        {
            Element childEl = (Element) children.item(x);

            //Build a class for the element.
            Object o = binding.createObject(childEl.getNamespaceURI(), 
                    childEl.getLocalName());

            //Parse the nodes.
            if(o instanceof SOAPObject)
            {
                ((SOAPObject) o).parse(binding, childEl);
            }

            //Add to the child list.
            bodyElements.add(o);
        }
    }
}
