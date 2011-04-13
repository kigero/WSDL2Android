package ${package};

import org.xmlpull.v1.XmlSerializer;
import java.io.IOException;
import org.w3c.dom.Element;
import android.util.Log;
import java.lang.reflect.Field;

public class SOAPObject
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

    public String getNamespace()
    {
        return null;
    }

    public void addAttributesToNode(XmlSerializer xml) throws IOException
    {

    }

    public void addElementsToNode(XmlSerializer xml) throws IOException
    {

    }

    public void parse(SOAPBinding binding, Element el)
    {

    }

    public static Object parseElement(SOAPBinding binding, Element el, Object parent, 
            String propName)
    {
        Object rtrn = null;

        if(parent == null)
        {
            return rtrn;
        }

        //Get the field on the parent object.
        Field field = null;
        try
        {
            field = parent.getClass().getDeclaredField(propName);
        }
        catch(Exception e)
        {
            if(binding.isLogEnabled())
            {
                Log.e(binding.getLogTag(), "Could not find field '" + propName
                        + "' on class '" + parent.getClass().getSimpleName() + "'.");
            }
            return rtrn;
        }

        try
        {
            rtrn = field.getType().newInstance();
        }
        catch(Exception e)
        {
            if(binding.isLogEnabled())
            {
                Log.e(binding.getLogTag(), "Could not create new instance of '"
                        + field.getType().getClass().getSimpleName() 
                        + "' for element field '" + propName + "' on class '" 
                        + parent.getClass().getSimpleName() + "'.");
            }
            return rtrn;
        }

        if(rtrn instanceof SOAPObject)
        {
            ((SOAPObject) rtrn).parse(binding, el);
        }
        else if(rtrn instanceof String)
        {
            rtrn = el.getTextContent();
        }
        //...etc...
        
        return rtrn;
    }

    public Object parseAttribute(SOAPBinding binding, String attValue, 
            Object parent, String propName)
    {
        Object rtrn = null;

        if(parent == null)
        {
            return rtrn;
        }

        //Get the field on the parent object.
        Field field = null;
        try
        {
            field = parent.getClass().getDeclaredField(propName);
        }
        catch(Exception e)
        {
            if(binding.isLogEnabled())
            {
                Log.e(binding.getLogTag(), "Could not find field '" + propName
                        + "' on class '" + parent.getClass().getSimpleName() + "'.");
            }
            return rtrn;
        }

        try
        {
            rtrn = field.getType().newInstance();
        }
        catch(Exception e)
        {
            if(binding.isLogEnabled())
            {
                Log.e(binding.getLogTag(), "Could not create new instance of '"
                        + field.getType().getClass().getSimpleName() 
                        + "' for attribute field '" + propName + "' on class '" 
                        + parent.getClass().getSimpleName() + "'.");
            }
            return rtrn;
        }

        if(rtrn instanceof String)
        {
            rtrn = attValue;
        }
        //...etc...
        
        return rtrn;

    }
}
