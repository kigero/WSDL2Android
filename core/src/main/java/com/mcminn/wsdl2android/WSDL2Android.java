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

    private List<File> wsdls = new ArrayList<File>();
    private String outputPackage = "wsdl2android";
    private String outputTypesPackage = "";
    private File outputDir = new File(".");

    private Configuration ftlConfig;

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
                        def.getPrefix(type.getNamespaceURI()), type.getLocalPart());
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
        Map<String, String> namespaces = def.getNamespaces();
        for(String prefix : namespaces.keySet())
        {
            Map<String, Object> bOpNamespace = new HashMap<String, Object>();
            bOpNamespace.put("prefix", prefix);
            bOpNamespace.put("uri", namespaces.get(prefix));
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

    private Map<String, Object> getTypeModel(XmlSchemaElement element, 
            String prefix)
    {
        Map<String, Object> rtrn = new HashMap<String, Object>();

        rtrn.put("package", outputTypesPackage);
        if(!outputPackage.equals(outputTypesPackage))
        {
            rtrn.put("importBindings", outputPackage);
        }

        rtrn.put("typeName", 
                buildTypeName(prefix, element.getQName().getLocalPart()));

        rtrn.put("namespace", element.getQName().getNamespaceURI());

        List<Map<String, Object>> atts = new ArrayList<Map<String, Object>>();

        XmlSchemaType type = element.getSchemaType();
        if(type instanceof XmlSchemaComplexType)
        {
            XmlSchemaComplexType complexType = (XmlSchemaComplexType) type;
            XmlSchemaObjectCollection col = complexType.getAttributes();
            for(int x = 0;x < col.getCount();x++)
            {
                XmlSchemaObject obj = col.getItem(x);
                if(obj instanceof XmlSchemaAttribute)
                {
                    XmlSchemaAttribute att = (XmlSchemaAttribute) obj;
                    
                    Map<String, Object> attModel = new HashMap<String, Object>();
                    attModel.put("name", att.getName());
                    attModel.put("javaType", "String");
                    atts.add(attModel);
                }
            }

            //Sequence
            /*
            XmlSchemaSequence seq = (XmlSchemaSequence) ct.getParticle();
            XmlSchemaObjectCollection col = seq.getItems();
            for(int x = 0;x < col.getCount();x++)
            {
                XmlSchemaObject obj = col.getItem(x);

                System.out.println(x + ": [" + obj.getClass().getSimpleName() 
                        + "] " + obj.toString("", 0)); 
            }
            */
        }
        else if(type instanceof XmlSchemaSimpleType)
        {

        }

        rtrn.put("atts", atts);
        return rtrn;
    }

    private Map<String, Object> getTypeModel(XmlSchemaType type,
            String prefix)
    {
        Map<String, Object> rtrn = new HashMap<String, Object>();

        rtrn.put("package", outputTypesPackage);
        if(!outputPackage.equals(outputTypesPackage))
        {
            rtrn.put("importBindings", outputPackage);
        }

        rtrn.put("typeName", 
                buildTypeName(prefix, type.getQName().getLocalPart()));

        rtrn.put("namespace", type.getQName().getNamespaceURI());


        return rtrn;
    }

    private Map<String, Map<String, Object>> getTypes(String baseURI, Element el, 
            String prefix) 
    {
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
                    getTypeModel((XmlSchemaElement) tableValuesI.next(), prefix);
                rtrn.put((String) typeModel.get("typeName"), typeModel);
            }

            table = ss.getSchemaTypes();
            tableValuesI = table.getValues();
            while(tableValuesI.hasNext())
            {
                Map<String, Object> typeModel =
                    getTypeModel((XmlSchemaType) tableValuesI.next(), prefix);
                rtrn.put((String) typeModel.get("typeName"), typeModel);
            }
        }
        catch(Exception e)
        {
            log.warning("Caught exception parsing schema: " + e.getMessage());
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

                types.putAll(getTypes(def.getDocumentBaseURI(), 
                            schemaEl.getElement(), "")); 

                Map<String, Vector<SchemaImport>> imports = schemaEl.getImports();
                
                for(String key : imports.keySet())
                {
                    Vector<SchemaImport> vecImports = imports.get(key);
                    for(SchemaImport schemaImport : vecImports)
                    {
                        types.putAll(getTypes(def.getDocumentBaseURI(), 
                                    schemaImport.getReferencedSchema().getElement(), 
                                    def.getPrefix(schemaImport.getNamespaceURI())));
                    }
                }

                List<SchemaReference> includes = schemaEl.getIncludes();
                for(SchemaReference schemaReference : includes)
                {
                    types.putAll(getTypes(def.getDocumentBaseURI(), 
                                schemaReference.getReferencedSchema().getElement(), 
                                schemaReference.getId())); 
                }

                List<SchemaReference> redefines = schemaEl.getRedefines();
                for(SchemaReference schemaReference : redefines)
                {
                    types.putAll(getTypes(def.getDocumentBaseURI(), 
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
