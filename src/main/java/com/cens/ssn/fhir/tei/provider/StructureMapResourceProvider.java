/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.ssn.fhir.tei.provider;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.transformer.BundleIniciarTransformer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.StructureMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class StructureMapResourceProvider implements IResourceProvider{

    private static final  Logger log = LoggerFactory.getLogger(StructureMapResourceProvider.class);
    FhirServerConfig fhirServerConfig;
    
    BundleIniciarTransformer bundleIniciarTransformer;

    public StructureMapResourceProvider() {
    }

    
    
    
    public StructureMapResourceProvider(FhirServerConfig fhirServerConfig, BundleIniciarTransformer iniciarTransformer) {
        this.fhirServerConfig = fhirServerConfig;
        this.bundleIniciarTransformer = iniciarTransformer;
    }
    
    

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return StructureMap.class;
    }
    
    @Operation(name = "$transform", type = StructureMap.class, manualResponse = true,manualRequest = true)
    public void manualInputAndOutput(HttpServletRequest theServletRequest, HttpServletResponse response)
      throws IOException {
        
        log.info("Entry StructureMap/$transform request.");
        
        
        Map<String, String[]> requestParams = theServletRequest.getParameterMap();
        String[] source = requestParams.get("source");
        if (source == null || source.length <= 0) {
            throw new InvalidRequestException("source parameter not found.");
        }
        //String collect = theServletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        //System.out.println("collect = " + collect);
        
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String res = "";
        
        if(source[0].equals("http://worldhealthorganization.github.io/tei/StructureMap/CoreDataSetIniciarToBundle")){
            String data = theServletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            res = bundleIniciarTransformer.buildBundleIniciar(data);
            
        }
       
        else{
            throw new UnprocessableEntityException("Map not available with canonical url "+source[0]);
        }
        out.print(res);
        out.flush(); 
  
    }
    
   
}
