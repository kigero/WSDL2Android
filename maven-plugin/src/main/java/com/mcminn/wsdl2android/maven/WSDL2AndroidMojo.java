package com.mcminn.wsdl2android.maven; 

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import com.mcminn.wsdl2android.WSDL2Android;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.util.Map;

/**
 * @goal generate
 */
public class WSDL2AndroidMojo extends AbstractMojo
{
    /**
     * @parameter
     */
    private FileSet[] wsdlsets;

    /**
     * @parameter
     */
    private File outputDir;

    /**
     * @parameter
     */
    private String outputPackage;

    /**
     * @parameter
     */
    private String outputTypesPackage;

    /**
     * @parameter
     */
    private Map<String, String> namespaceMap;
    
    public void execute() throws MojoExecutionException
    {
        WSDL2Android wsdl2android = new WSDL2Android();

        if(outputDir != null)
        {
            wsdl2android.setOutputDir(outputDir);
        }

        if(outputPackage != null)
        {
            wsdl2android.setOutputPackage(outputPackage);
        }

        if(outputTypesPackage != null)
        {
            wsdl2android.setOutputTypesPackage(outputTypesPackage);
        }

        if(namespaceMap != null)
        {
            for(String prefix : namespaceMap.keySet())
            {
                wsdl2android.addNamespace(prefix, namespaceMap.get(prefix));
            }
        }

        if(wsdlsets != null && wsdlsets.length > 0)
        {
            FileSetManager fileSetManager = new FileSetManager();
            for(FileSet wsdlset : wsdlsets)
            {
                String[] includedFiles = fileSetManager.getIncludedFiles(wsdlset);
                for(String includedFile : includedFiles)
                {
                    File wsdl = new File(wsdlset.getDirectory() + "/" + includedFile);
                    wsdl2android.addWSDL(wsdl);
                }
            }
            wsdl2android.generate();
        }
        else
        {
            System.out.println("No WSDL sets defined.");
        }
    }
}
