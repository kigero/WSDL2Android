package ${package};

import android.util.Log;
import java.util.Map;
import java.util.HashMap;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.HttpEntity;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public abstract class SOAPBinding
{
    private static final String TYPES_PACKAGE = "${typePackage}";

    private String endpoint = "";
    private Map<String, String> namespaces;
    private String logTag = "wsdl2android";
    private SOAPBindingDefaults defaults = new Defaults();
    private static Map<String, Header> cookies = new HashMap<String, Header>();

    public SOAPBinding(String endpoint)
    {
        if(endpoint != null)
        {
            this.endpoint = endpoint;
        }

        namespaces = getNamespaces();
    }

    public void setLogTag(String logTag)
    {
        this.logTag = logTag;
    }

    public String getLogTag()
    {
        return logTag;
    }

    public boolean isLogEnabled()
    {
        return logTag != null;
    }

    public void setDefaults(SOAPBindingDefaults defaults)
    {
        if(defaults != null)
        {
            this.defaults = defaults;
        }
    }

    public SOAPEnvelope makeRequest(Map<String, SOAPObject> bodyElements)
        throws IOException
    {
        String envelope = buildEnvelope(bodyElements);
        if(isLogEnabled())
        {
            Log.d(logTag, "Request: \n" + envelope);
        }

        String response = sendXmlToServer(envelope, "login");

        ResponseParser respParser = new ResponseParser(response);
        if(isLogEnabled())
        {
            Log.d(logTag, "Response: \n" + respParser.getXmlAsPrettyString());
        }

        SOAPEnvelope respObj = respParser.parse();
        if(isLogEnabled())
        {
            Log.d(logTag, respObj.bodyElements.size() + " body elements.");
            for(Object o : respObj.bodyElements)
            {
                Log.d(logTag, "Parsed root object: " + (o == null ? "null" : o.getClass()));
            }
        }

        return respObj;
    }

    public String buildEnvelope(Map<String, SOAPObject> bodyElements) 
        throws IOException
    {
        return SOAPEnvelope.getXML(namespaces, null, bodyElements);
    }

    public String getMIMEType()
    {
        return "text/xml";
    }

    public String getNSPrefix(String namespace)
    {
        //Note this only works if prefixes map to a single namespace...
        for(Map.Entry<String, String> entry : getNamespaces().entrySet())
        {
            if(entry.getValue().equals(namespace))
            {
                return entry.getKey();
            }
        }

        return null;
    }

    public String getNS(String prefix)
    {
        return getNamespaces().get(prefix);
    }

    public Object createObject(String uri, String localName)
    {
        Object rtrn = null;
        String clsName = TYPES_PACKAGE + "." 
            + getNSPrefix(uri) + "_" 
            + localName;
        try
        {
            rtrn = Class.forName(clsName).newInstance();
        }
        catch(Exception e)
        {
            if(isLogEnabled())
            {
                Log.e(logTag, "Could not create class '" + clsName + ".");
            }
        }

        return rtrn;
    }

    private String sendXmlToServer(String envelope, String action) throws IOException
    {
        HttpClient client = defaults.getClient();
        HttpPost post = new HttpPost(endpoint);

        byte[] body = envelope.getBytes("UTF-8");

        post.addHeader("User-Agent", "wsdl2android");
        post.addHeader("SOAPAction", action);
        post.addHeader("Content-Type", getMIMEType() + "; charset=utf-8");
        for(Header cookieHeader : cookies.values())
        {
            post.addHeader(cookieHeader);
        }
        
        //TODO add custom headers
        post.setEntity(new ByteArrayEntity(body));

        HttpResponse response = client.execute(post, new BasicHttpContext());

        //Look for cookies
        Header[] cookieHeaders = response.getHeaders("Set-Cookie");
        for(Header h : cookieHeaders)
        {
            if(isLogEnabled())
            {
                //Get the name of the cookie.
                String cookieName = h.getValue();
                cookieName = cookieName.substring(0, cookieName.indexOf("="));

                Header cookieHeader = new BasicHeader("Cookie", h.getValue());
                cookies.put(cookieName, cookieHeader);
            }
        }

        HttpEntity respEnt = response.getEntity();
        //Log.d("testclient", "response type: " + respEnt.getContentType().toString());
        String line = "";
        StringBuilder total = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(respEnt.getContent()));
        while((line = rd.readLine()) != null)
        {
            total.append(line);
        }

        return total.toString();
    }
    
    public class ResponseParser
    {
        private Document doc;

        public ResponseParser(String xml)
        {
            try
            {
               DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
               dbFactory.setNamespaceAware(true);
               DocumentBuilder builder = dbFactory.newDocumentBuilder();
               doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            }
            catch(Exception e)
            {
                throw new IllegalArgumentException("Bad XML response.", e);
            }
        }

        public String getXmlAsPrettyString()
        {
            String rtrn = "";
            try
            {		
                TransformerFactory tFactory = TransformerFactory.newInstance();
                Transformer transformer = tFactory.newTransformer();

                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                StringWriter sw = new StringWriter();
                StreamResult result = new StreamResult(sw);

                DOMSource source = new DOMSource(doc);

                transformer.transform(source, result);

                rtrn = sw.toString();
            }
            catch(TransformerException te)
            {
                //
            }

            return rtrn;
        }

        public SOAPEnvelope parse()
        {
            if(doc == null)
            {
                //TODO throw an exception or something...
                return null;
            }

            Element root = doc.getDocumentElement();
            SOAPEnvelope rtrn = new SOAPEnvelope();
            rtrn.parse(SOAPBinding.this, root);
            return rtrn;
        }
    }

    public interface SOAPBindingDefaults
    {
        public HttpClient getClient();
    }

    private class Defaults implements SOAPBindingDefaults
    {
        public HttpClient getClient()
        {
            return new DefaultHttpClient();
        }
    }

    public abstract Map<String, String> getNamespaces();
}
