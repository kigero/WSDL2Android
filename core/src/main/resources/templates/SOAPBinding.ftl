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
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import android.util.Xml;

public abstract class SOAPBinding
{
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

    public void setDefaults(SOAPBindingDefaults defaults)
    {
        if(defaults != null)
        {
            this.defaults = defaults;
        }
    }

    public void makeRequest(Map<String, SOAPObject> bodyElements)
        throws IOException
    {
        String envelope = buildEnvelope(bodyElements);
        if(logTag != null)
        {
            Log.d(logTag, "Request: \n" + envelope);
        }

        String response = sendXmlToServer(envelope, "login");
        ResponseParser respParser = new ResponseParser();
        
        if(logTag != null)
        {
            respParser.setPrettify(true);
        }

        try
        {
            Xml.parse(response, respParser);

            if(logTag != null)
            {
                Log.d(logTag, "Response: \n" + respParser.getPrettyString());
            }
        }
        catch(SAXException saxe)
        {
            throw new IOException("Could not parse response.", saxe);
        }
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
            if(logTag != null)
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

    private class ResponseParser extends DefaultHandler
    {
        private int indent = 0;
        private StringBuilder prettyXML;

        public void setPrettify(boolean prettify)
        {
            if(prettify)
            {
                prettyXML = new StringBuilder();
            }
        }

        public String getPrettyString()
        {
            if(prettyXML != null)
            {
                return prettyXML.toString();
            }

            return null;
        }

        public void startDocument()
        {
            indent = 0;
        }

        private void appendIndentString()
        {
            if(prettyXML != null)
            {
                for(int x = 0;x < indent;x++)
                {
                    prettyXML.append("    ");
                }
            }
        }

        public void startElement(String uri, String localName, String qName, 
                Attributes attributes)
        {
            if(prettyXML != null)
            {
                appendIndentString();
                prettyXML.append("<" + qName);
                for(int x = 0;x < attributes.getLength();x++)
                {
                    String attQName = attributes.getQName(x);
                    String name = attributes.getLocalName(x);
                    if(!uri.equals(attributes.getURI(x)))
                    {
                        name = attQName;
                    }

                    prettyXML.append(" " + name + "=\"" +
                            attributes.getValue(attQName) + "\"");
                }
                prettyXML.append(">\n");
                indent++;
            }
        }

        public void endElement(String uri, String localName, String qName)
        {
            if(prettyXML != null)
            {
                indent--;
                appendIndentString();
                prettyXML.append("</" + qName + ">\n");
            }
        }

        public void characters(char[] ch, int start, int length)
        {
            if(prettyXML != null)
            {
                String str = new String(ch);
                if(!str.trim().equals(""))
                {
                    prettyXML.append(str);
                }
            }
        }

        public void processingInstruction(String target, String data)
        {
            if(prettyXML != null)
            {
                appendIndentString();
                prettyXML.append("<?" + target + " " + data + "?>\n");
            }
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
