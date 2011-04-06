package com.mcminn.wsdl2android;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.File;
import com.mcminn.wsdl2android.util.LogUtil;
import javax.wsdl.*;
import javax.wsdl.factory.*;
import javax.wsdl.xml.*;
import javax.wsdl.extensions.*;
import javax.wsdl.extensions.schema.*;
import java.util.Map;
import java.util.HashMap;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import freemarker.template.*;
import java.io.IOException;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.*;
import java.util.Iterator;
import org.w3c.dom.*;
import java.util.Vector;

public class WSDL2Android
{
    private static Logger log;
    private static Map<String, String> wsdlTypeToJavaType = new HashMap<String, String>();
    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    private List<File> wsdls = new ArrayList<File>();
    private String outputPackage = "wsdl2android";
    private String outputTypesPackage = "";
    private File outputDir = new File(".");

    private Configuration ftlConfig;
    private Map<String, String> namespaceMap = new HashMap<String, String>();

    static
    {
        wsdlTypeToJavaType.put("boolean", "Boolean");
        wsdlTypeToJavaType.put("byte", "Byte");
        wsdlTypeToJavaType.put("int", "Integer");
        wsdlTypeToJavaType.put("integer", "Integer");
        wsdlTypeToJavaType.put("nonNegativeInteger", "Integer");
        wsdlTypeToJavaType.put("positiveInteger", "Integer");
        wsdlTypeToJavaType.put("unsignedByte", "Byte");
        wsdlTypeToJavaType.put("unsignedInt", "Integer");
        wsdlTypeToJavaType.put("unsignedLong", "Long");
        wsdlTypeToJavaType.put("unsignedShort", "Short");
        wsdlTypeToJavaType.put("double", "Double");
        wsdlTypeToJavaType.put("long", "Long");
        wsdlTypeToJavaType.put("short", "Short");
        wsdlTypeToJavaType.put("float", "Float");
        wsdlTypeToJavaType.put("dateTime", "java.util.Calendar");
        wsdlTypeToJavaType.put("date", "java.util.Calendar");
        wsdlTypeToJavaType.put("time", "java.util.Calendar");
        wsdlTypeToJavaType.put("duration", "Integer");
        wsdlTypeToJavaType.put("base64Binary", "Byte[]");
        wsdlTypeToJavaType.put("decimal", "java.math.BigDecimal");
        wsdlTypeToJavaType.put("QName", "javax.xml.namespace.QName");
        wsdlTypeToJavaType.put("anyURI", "java.net.URI");
        wsdlTypeToJavaType.put("string", "String");
        wsdlTypeToJavaType.put("normalizedString", "String");
        wsdlTypeToJavaType.put("token", "String");
        wsdlTypeToJavaType.put("language", "String");
        wsdlTypeToJavaType.put("Name", "String");
        wsdlTypeToJavaType.put("NCName", "String");
        wsdlTypeToJavaType.put("anyType", "String");
        wsdlTypeToJavaType.put("ID", "String");
        wsdlTypeToJavaType.put("ENTITY", "String");
        wsdlTypeToJavaType.put("IDREF", "String");
        wsdlTypeToJavaType.put("hexBinary", "Byte[]");
    }

    public WSDL2Android()
    {
        log = LogUtil.buildLogger(WSDL2Android.class.getName());
    }

    public void addWSDL(File wsdl)
    {
        if(wsdl != null && wsdl.exists())
        {
            if(!wsdls.contains(wsdl))
            {
                wsdls.add(wsdl);
            }
        }
    }

    public void addWSDLs(List<File> wsdls)
    {
        if(wsdls != null)
        {
            for(File wsdl : wsdls)
            {
                addWSDL(wsdl);
            }
        }
    }

    private void configureOutputTypePackage(String wsdlPackage, String typePackage)
    {
        if(typePackage == null || typePackage.equals(""))
        {
            outputTypesPackage = wsdlPackage;
        }
        else if(typePackage.startsWith("."))
        {
            outputTypesPackage = wsdlPackage + typePackage;
        }
        else
        {
            outputTypesPackage = typePackage;
        }
    }

    public void setOutputDir(File outputDir)
    {
        if(outputDir == null)
        {
            throw new IllegalArgumentException("outputDir cannot be null.");
        }

        this.outputDir = outputDir;
    }

    public void setOutputPackage(String outputPackage)
    {
        if(outputPackage == null)
        {
            throw new IllegalArgumentException("outputPackage cannot be null.");
        }

        this.outputPackage = outputPackage;
        configureOutputTypePackage(outputPackage, outputTypesPackage);
    }

    public void setOutputTypesPackage(String outputTypesPackage)
    {
        if(outputTypesPackage == null)
        {
            throw new IllegalArgumentException("outputTypesPackage cannot be null.");
        }

        configureOutputTypePackage(outputPackage, outputTypesPackage);
    }

    private void outputConfiguration()
    {
        log.config("Output dir: " + outputDir.getAbsolutePath());
        log.config("Output package: " + outputPackage);
        log.config("# of WSDLs: " + wsdls.size());
    }

    private File buildPackageOutputDir()
    {
        return new File(outputDir.getAbsolutePath() + "/"
                + outputPackage.replace(".", "/"));
    }

    private File buildPackageTypesOutputDir()
    {
        return new File(outputDir.getAbsolutePath() + "/"
                + outputTypesPackage.replace(".", "/"));
    }

    public void generate()
    {
        outputConfiguration();

        if(wsdls.size() == 0)
        {
            log.info("No WSDLs provided.");
        }
        else
        {
            File pkgOutputDir = buildPackageOutputDir();
            if(!pkgOutputDir.exists())
            {
                pkgOutputDir.mkdirs();
            }

            File pkgTypesOutputDir = buildPackageTypesOutputDir();
            if(!pkgTypesOutputDir.exists())
            {
                pkgTypesOutputDir.mkdirs();
            }

            copyBaseClasses(pkgOutputDir);
            for(File wsdl : wsdls)
            {
                generate(wsdl, pkgOutputDir, pkgTypesOutputDir);
            }
        }
    }

    private void copyBaseClasses(File outputToDir)
    {
        Map<String, Object> baseModel = new HashMap<String, Object>();
        baseModel.put("package", outputPackage);

        String[] baseTemplateNames = {"SOAPBinding", "SOAPEnvelope", "SOAPObject"};
        try
        {
            for(String baseTemplateName : baseTemplateNames)
            {
                File outputFile = new File(outputToDir.getAbsolutePath() + "/"
                        + baseTemplateName + ".java");

                Template typeTemplate = getTemplate(baseTemplateName + ".ftl");

                Writer out = new OutputStreamWriter(
                        new FileOutputStream(outputFile, false));
                typeTemplate.process(baseModel, out);
                out.flush();
                out.close();
            }
        }
        catch(Exception e)
        {
            //TODO do something!!!
            e.printStackTrace();
        }
    }

    private Configuration getFTLConfig()
    {
        if(ftlConfig == null)
        {
            ftlConfig = new Configuration();
            ftlConfig.setClassForTemplateLoading(getClass(), "/templates");
            ftlConfig.setObjectWrapper(new DefaultObjectWrapper());
        }

        return ftlConfig;
    }

    private Template getTemplate(String name) throws IOException
    {
        return getFTLConfig().getTemplate(name);
    }

    private static String buildTypeName(String prefix, String type)
    {
        String rtrn = type;
        if(prefix != null && !prefix.equals(""))
        {
            rtrn = prefix + "_" + rtrn;
        }

        return rtrn;
    }

    private Map<String, Object> getBindingOperationModel(Definition def, BindingOperation bOp)
    {
        Map<String, Object> rtrn = new HashMap<String, Object>();

        Operation op = bOp.getOperation();
        Message inMsg = op.getInput().getMessage();

        rtrn.put("name", op.getName());

        StringBuffer inputSB = new StringBuffer();
        List<Map<String, Object>> params = new ArrayList<Map<String, Object>>();
        Part[] parts = (Part[]) inMsg.getParts().values().toArray(new Part[0]);
        for(int x = 0;x < parts.length;x++)
        {
            Part part = parts[x];
            QName type = part.getElementName();
            if(type != null)
            {
                String typeName = buildTypeName(
                        getPrefix(type.getNamespaceURI()), type.getLocalPart());
                String partName = part.getName();

                inputSB.append(typeName);
                        inputSB.append(" ");
                inputSB.append(partName);
                if(x < parts.length - 1)
                {
                    inputSB.append(", ");
                }

                Map<String, Object> param = new HashMap<String, Object>();
                param.put("qType", typeName);
                param.put("type", type.getLocalPart());
                param.put("name", partName);
                params.add(param);
            }
        }
        rtrn.put("paramString", inputSB.toString());
        rtrn.put("params", params);

        return rtrn;
    }

    private Map<String, Object> getBindingModel(Definition def, Binding binding)
    {
        Map<String, Object> rtrn = new HashMap<String, Object>();

        rtrn.put("package", outputPackage);
        if(!outputPackage.equals(outputTypesPackage))
        {
            rtrn.put("importTypes", outputTypesPackage);
        }

        rtrn.put("bindingName", binding.getQName().getLocalPart());

        List<Map<String, Object>> bOpNamespaces = new ArrayList<Map<String, Object>>();
        for(String prefix : namespaceMap.keySet())
        {
            Map<String, Object> bOpNamespace = new HashMap<String, Object>();
            bOpNamespace.put("prefix", prefix);
            bOpNamespace.put("uri", namespaceMap.get(prefix));
            bOpNamespaces.add(bOpNamespace);
        }
        rtrn.put("namespaces", bOpNamespaces);

        List<Map<String, Object>> bOpModels = new ArrayList<Map<String, Object>>();
        List<BindingOperation> bOps = binding.getBindingOperations();
        for(BindingOperation bOp : bOps)
        {
            bOpModels.add(getBindingOperationModel(def, bOp));
        }

        rtrn.put("bOps", bOpModels);

        return rtrn;
    }

    private void writeBindings(Definition def, File outputToDir)
        throws Exception
    {
        Map<QName, Service> services = def.getServices();
        for(Service service : services.values())
        {
            Map<String, Port> ports = service.getPorts();
            for(Port port : ports.values())
            {
                Map<String, Object> bindingModel = getBindingModel(def,
                        port.getBinding());

                Template bindingTemplate = getTemplate("Binding.ftl");

                File outputFile = new File(outputToDir.getAbsolutePath() + "/"
                        + bindingModel.get("bindingName") + ".java");

                Writer out = new OutputStreamWriter(
                        new FileOutputStream(outputFile, false));
                bindingTemplate.process(bindingModel, out);
                out.flush();
                out.close();
            }
        }
    }

    private String getTypeFromQName(QName qName)
    {
        String typeName = null;

        if(qName != null)
        {
            //If the namespace is xsd, assume it's a base type.
            if(XSD_NAMESPACE.equals(qName.getNamespaceURI()))
            {
                typeName = qName.getLocalPart();
                if(typeName != null)
                {
                    typeName = wsdlTypeToJavaType.get(typeName);
                }
            }
            else
            {
                typeName = buildTypeName(getPrefix(qName.getNamespaceURI()), qName.getLocalPart());
            }                    
        }

        if(typeName == null || typeName.equals(""))
        {
            typeName = "/* Couldn't find real type */ String";
        }

        return typeName;
    }

    private Map<String, Object> getModelForAttribute(XmlSchemaAttribute att)
    {
        Map<String, Object> attModel = new HashMap<String, Object>();
        attModel.put("name", att.getName());
        /*
        System.out.println("--" + att.getName() + "--");
        System.out.println("type: " + att.getAttributeType());
        System.out.println("qname: " + att.getQName());
        System.out.println("schema type: " + att.getSchemaType());
        System.out.println("schema type name: " + att.getSchemaTypeName());
        System.out.println("----------");
        */

        //It looks like the schema type name gives you the right java type.
        attModel.put("javaType", getTypeFromQName(att.getSchemaTypeName()));
        return attModel;
    }

    private Map<String, Object> getModelForSequenceElement(XmlSchemaElement el)
    {
        Map<String, Object> elModel = new HashMap<String, Object>();
        elModel.put("name", el.getName());
        /*
        System.out.println("--" + el.getName() + "--");
        System.out.println("qname: " + el.getQName());
        System.out.println("schema type: " + el.getSchemaType());
        System.out.println("schema type name: " + el.getSchemaTypeName());
        System.out.println("----------");
        */

        //It looks like the schema type name gives you the right type.
        elModel.put("javaType", getTypeFromQName(el.getSchemaTypeName()));

        return elModel;
    }

    private void getPropertiesFromComplexType(XmlSchemaComplexType type, Map<String, Object> masterModel)
    {
       System.out.println("Complex type: " + type.toString());

       //Get the attributes on the type.
       XmlSchemaObjectCollection col = type.getAttributes();
       if(col != null)
       {
           List<Map<String, Object>> atts = new ArrayList<Map<String, Object>>();

           for(int x = 0;x < col.getCount();x++)
           {
               XmlSchemaObject obj = col.getItem(x);
               if(obj instanceof XmlSchemaAttribute)
               {
                   atts.add(getModelForAttribute((XmlSchemaAttribute) obj));
               }
           }

           masterModel.put("atts", atts);
       }

       //Get the sequence elements.
       XmlSchemaSequence seq = (XmlSchemaSequence) type.getParticle();
       if(seq != null)
       {
           col = seq.getItems();
           List<Map<String, Object>> sequenceModels = new ArrayList<Map<String, Object>>();

           for(int x = 0;x < col.getCount();x++)
           {
               XmlSchemaObject obj = col.getItem(x);
               if(!(obj instanceof XmlSchemaElement))
               {
                   continue;
               }

               XmlSchemaElement seqEl = (XmlSchemaElement) obj;
               sequenceModels.add(getModelForSequenceElement(seqEl));
           }

           masterModel.put("elements", sequenceModels);
       }
    }

    private void getPropertiesFromSimpleType(XmlSchemaSimpleType type, Map<String, Object> masterModel)
    {
        List<Map<String, Object>> rtrn = new ArrayList<Map<String, Object>>();

        System.out.println("Simple type: " + type.toString());
    }

    private void addPropertiesFromType(XmlSchemaType type, Map<String, Object> masterModel)
    {
        if(type instanceof XmlSchemaComplexType)
        {
            getPropertiesFromComplexType((XmlSchemaComplexType) type, masterModel);
        }
        else if(type instanceof XmlSchemaSimpleType)
        {
            getPropertiesFromSimpleType((XmlSchemaSimpleType) type, masterModel);
        }
    }

    private Map<String, Object> getTypeModel(Definition def, XmlSchemaType type, String prefix)
    {
        Map<String, Object> rtrn = new HashMap<String, Object>();

        rtrn.put("package", outputTypesPackage);
        if(!outputPackage.equals(outputTypesPackage))
        {
            rtrn.put("importBindings", outputPackage);
        }

        String typeName = buildTypeName(prefix, type.getName());
        rtrn.put("typeName", typeName);
        System.out.println("Processing type: " + typeName);

        if(type.getQName() == null)
        {
            rtrn.put("namespace", "");
        }
        else
        {
            rtrn.put("namespace", type.getQName().getNamespaceURI());
        }

        //Find the properties on the master type.
        addPropertiesFromType(type, rtrn);

        return rtrn;
    }

    private Map<String, Object> getTypeModel(Definition def, XmlSchemaElement element, String prefix)
    {
        Map<String, Object> rtrn = new HashMap<String, Object>();

        rtrn.put("package", outputTypesPackage);
        if(!outputPackage.equals(outputTypesPackage))
        {
            rtrn.put("importBindings", outputPackage);
        }

        if(element.getName() == null)
        {
            System.out.println(element.getSourceURI() + ": " + element.getLineNumber() + " ::: " + element.toString());
        }
        String typeName = buildTypeName(prefix, element.getName());
        rtrn.put("typeName", typeName);
        System.out.println("Processing element: " + typeName);

        if(element.getQName() == null)
        {
            rtrn.put("namespace", "");
        }
        else
        {
            rtrn.put("namespace", element.getQName().getNamespaceURI());
        }

        //Find the properties on the master type.
        addPropertiesFromType(element.getSchemaType(), rtrn);

        return rtrn;
    }

    private Map<String, Map<String, Object>> getTypes(Definition def, Element el, 
            String prefix) 
    {
        String baseURI = def.getDocumentBaseURI();
        Map<String, Map<String, Object>> rtrn = 
            new HashMap<String, Map<String, Object>>();

        try
        {
            XmlSchemaCollection schemaCol2 = new XmlSchemaCollection();
            schemaCol2.setBaseUri(baseURI);

            XmlSchema ss = schemaCol2.read(el);

            XmlSchemaObjectTable table = ss.getElements();
            Iterator tableValuesI = table.getValues();
            while(tableValuesI.hasNext())
            {
                Map<String, Object> typeModel =
                    getTypeModel(def, (XmlSchemaElement) tableValuesI.next(), prefix);
                rtrn.put((String) typeModel.get("typeName"), typeModel);
            }

            table = ss.getSchemaTypes();
            tableValuesI = table.getValues();
            while(tableValuesI.hasNext())
            {
                Map<String, Object> typeModel =
                    getTypeModel(def, (XmlSchemaType) tableValuesI.next(), prefix);
                rtrn.put((String) typeModel.get("typeName"), typeModel);
            }
        }
        catch(Exception e)
        {
            log.warning("Caught exception parsing schema: " + e.getMessage());
            log.throwing("WSDL2Android", "getTypes", e);
        }

        return rtrn;
    }

    private void writeTypes(Definition def, File outputToDir)
        throws Exception
    {
        Types typesEl = def.getTypes();
        
        Map<String, Map<String, Object>> types = 
            new HashMap<String, Map<String, Object>>();

        List<ExtensibilityElement> elements = typesEl.getExtensibilityElements();
        for(ExtensibilityElement el : elements)
        {
            if(el instanceof Schema)
            {
                Schema schemaEl = (Schema) el;

                types.putAll(getTypes(def, 
                            schemaEl.getElement(), "")); 

                Map<String, Vector<SchemaImport>> imports = schemaEl.getImports();
                
                for(String key : imports.keySet())
                {
                    Vector<SchemaImport> vecImports = imports.get(key);
                    for(SchemaImport schemaImport : vecImports)
                    {
                        types.putAll(getTypes(def, 
                                    schemaImport.getReferencedSchema().getElement(), 
                                    getPrefix(schemaImport.getNamespaceURI())));
                    }
                }

                List<SchemaReference> includes = schemaEl.getIncludes();
                for(SchemaReference schemaReference : includes)
                {
                    types.putAll(getTypes(def, 
                                schemaReference.getReferencedSchema().getElement(), 
                                schemaReference.getId())); 
                }

                List<SchemaReference> redefines = schemaEl.getRedefines();
                for(SchemaReference schemaReference : redefines)
                {
                    types.putAll(getTypes(def, 
                                schemaReference.getReferencedSchema().getElement(), 
                                schemaReference.getId())); 
                }
            }
        }

        for(Map<String, Object> typeModel : types.values())
        {
            File outputFile = new File(outputToDir.getAbsolutePath() + "/"
                    + typeModel.get("typeName") + ".java");

            Template typeTemplate = getTemplate("Type.ftl");

            Writer out = new OutputStreamWriter(
                    new FileOutputStream(outputFile, false));
            typeTemplate.process(typeModel, out);
            out.flush();
            out.close();
        }
    }

    public void addNamespace(String namespace)
    {
        if(!namespaceMap.containsKey(namespace))
        {
            int count = 0;
            while(namespaceMap.containsValue("ns" + count))
            {
                count++;
            }
            namespaceMap.put(namespace, "ns" + count);
        }
    }

    public void addNamespace(String prefix, String namespace)
    {
        namespaceMap.put(namespace, prefix);
    }

    private String getPrefix(String namespace)
    {
        return namespaceMap.get(namespace);
    }

    private void generate(File wsdlFile, File outputToDir, File outputTypesToDir)
    {
        log.config("Generating code for: " + wsdlFile.getAbsolutePath());
        try
        {
            WSDLFactory factory = WSDLFactory.newInstance();
            WSDLReader reader = factory.newWSDLReader();

            //reader.setFeature("javax.wsdl.verbose", true);
            reader.setFeature("javax.wsdl.importDocuments", true);

            Definition def = reader.readWSDL(null, wsdlFile.getAbsolutePath());

            for(Object o : def.getNamespaces().values())
            {
                addNamespace((String) o);
            }

            writeBindings(def, outputToDir);
            writeTypes(def, outputTypesToDir);            
        }
        catch(Exception e)
        {
            //TODO do something!!!
            e.printStackTrace();
        }
    }
}
