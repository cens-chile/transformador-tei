/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.fhirServer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.config.ThreadPoolFactoryConfig;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.ssn.fhir.tei.provider.StructureMapResourceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Configuration
// allow users to configure custom packages to scan for additional beans
@ComponentScan(basePackages = {"com.cens.ssn.fhir.tei"})
@Import(ThreadPoolFactoryConfig.class)
public class TEIFhirRestfulServer {
    
    private static final long serialVersionUID = 1L;
    
    
        
    @Bean
    public RestfulServer restfulServer(ApplicationContext appContext, FhirServerConfig conf,
            StructureMapResourceProvider structureMapResourceProvider){
        RestfulServer fhirServer = new RestfulServer(FhirContext.forR4Cached());
        FhirContext fhirContext = conf.getFhirContext();
        
        //fhirServer.registerProvider(pmp);
        fhirServer.registerProvider(structureMapResourceProvider);
        CustomOpenAPI openApiInterceptor = new CustomOpenAPI();
        fhirServer.registerInterceptor(openApiInterceptor);
        
        
        return fhirServer;

    }

    @Bean
    public HibernatePropertiesCustomizer jsonFormatMapper(final ObjectMapper objectMapper) {
        return properties -> properties.put(AvailableSettings.JSON_FORMAT_MAPPER, new JacksonJsonFormatMapper(objectMapper));
    }
    
    @Bean
    public FhirServerConfig config() {
        return new FhirServerConfig();
    }
    
    
}
