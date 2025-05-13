/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.config;

import ca.uhn.fhir.context.FhirContext;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
public class FhirServerConfig {
    
    
    FhirContext fhirContext;
    
    
    public FhirServerConfig(){
        fhirContext = FhirContext.forR4();       
    }

    public FhirContext getFhirContext() {
        return fhirContext;
    }

    public void setFhirContext(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }
    
    
    
    
}
