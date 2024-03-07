package com.genesis;

import lombok.extern.slf4j.Slf4j;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class Main {
    
    
    private static final String INB_XML_REQUEST_SCHEMA_FILE_NAME = "INTTRABooking2Request_V1.8.xsd";
    private static final SchemaFactory xmlSchemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    private static final Schema xmlSchema;
    
    static {
        Schema tempVar = null;
        try {
            Optional<Schema> result = getXMLSchema();            
            if(result.isPresent()) {
                tempVar = result.get();
                log.info("Schema resolution successful");
            }
        } catch(URISyntaxException ex){
            log.error("URISyntaxException occurred. Message = {}", ex.getMessage());
        } catch(IOException ex){
            log.error("IOException occurred. Message = {}", ex.getMessage());
        } catch(Exception ex){
            log.error("Exception occurred. Message = {}", ex.getMessage());   
        }
        xmlSchema = tempVar;
    }
    
    private static Optional<Schema> getXMLSchema() throws URISyntaxException, IOException, SAXException{
        Optional<Schema> schemaResult = Optional.empty(); 
        // get the transformer JAR contents
        List<Path> result = getPathsFromResourceJAR(".");
        log.info("Number of files in transformer jar = {}", result.size());
        log.info("Transformer JAR files are {}", Arrays.toString(result.toArray()));
        ClassLoader loader = Main.class.getClassLoader();
        try (InputStream is = loader.getResourceAsStream(INB_XML_REQUEST_SCHEMA_FILE_NAME)) {
            if (is != null) {
                schemaResult = Optional.ofNullable(xmlSchemaFactory.newSchema(new StreamSource(is)));
            }
        }
        
  /*      for(Path path : result){
            String fileName = path.toString();
            if(fileName.startsWith("/")){
                fileName = fileName.substring(1);
            }
            log.debug("filename = {}", fileName);
            if(INB_XML_REQUEST_SCHEMA_FILE_NAME.equalsIgnoreCase(fileName)){
                ClassLoader loader = Main.class.getClassLoader();
                try (InputStream is = loader.getResourceAsStream(fileName)) {
                    if(is != null){
                        schemaResult = Optional.ofNullable(xmlSchemaFactory.newSchema(new StreamSource(is))); 
                    }
                }
                break;
            }
        }*/
        return schemaResult;
    }
    
    // Get all paths from a folder that inside the JAR file
    private static List<Path> getPathsFromResourceJAR(String folder)
        throws URISyntaxException {
        
        List<Path> result = null;
        
        // get path of the current running JAR
        String jarPath = Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .toURI()
            .getPath();
        log.info("JAR Path :" + jarPath);
        
        // walk the JAR 
        URI uri = URI.create("jar:file:" + jarPath);
        
        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            try(Stream<Path> entries = Files.walk(fs.getPath(folder))){
                result = entries.filter(Files::isRegularFile).collect(Collectors.toList());
            }
        } catch(IOException ioe){
            log.error("IOException occurred while traversing the transformer.jar. Message = {}", ioe.getMessage());
        }
        
        return result;
        
        /**
         * when called as List<Path> result = getPathsFromResourceJAR(".");
         * [/INTTRABooking2Request_V1.8.xsd, /com/genesis/Main.class, /META-INF/maven/com.genesis/TestProject/pom.properties, /META-INF/maven/com.genesis/TestProject/pom.xml, /META-INF/MANIFEST.MF]
         *
         * when called as List<Path> result = getPathsFromResourceJAR("/");
         * [/INTTRABooking2Request_V1.8.xsd, /com/genesis/Main.class, /META-INF/maven/com.genesis/TestProject/pom.properties, /META-INF/maven/com.genesis/TestProject/pom.xml, /META-INF/MANIFEST.MF]
         */
    }
    
    public static void main(String[] args) {
        
        System.out.println("Start .. ");
        ClassLoader loader = Main.class.getClassLoader();
        InputStream input = loader.getResourceAsStream("inboundexample.xml");
        if(xmlSchema != null && input != null){
            try {
                Validator validator = xmlSchema.newValidator();
                validator.validate(new StreamSource(input));
                
            } catch (IOException | SAXException e) {
                e.printStackTrace();
            }
        }
        
    }
}