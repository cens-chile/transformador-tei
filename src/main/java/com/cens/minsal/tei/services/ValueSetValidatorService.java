/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.services;

import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport.CodeValidationResult;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Service
public class ValueSetValidatorService {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ValueSetValidatorService.class);
    IValidationSupport validationSupport;
    ValidationSupportContext validationSupportContext;

    public ValueSetValidatorService(IValidationSupport validationSupport) {
        this.validationSupport = validationSupport;
        this.validationSupportContext = new ValidationSupportContext(validationSupport);
    }
    
    
    public void customMethod() {
      String comunaCode = "13101";
      String realDisplay = "Santiago ";
      String system = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodComunasCL";
      String valueset = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosComunaCL";
      if(isValueSetSupported(valueset)){
        String display = validateCode(system, comunaCode, null, valueset);
          System.out.println("display = " + display);
        if (display != null) {
          log.info("sin display: {}" ,display);
        }
      }
    }
    
    
    public String validateCode(String theCodeSystem, String theCode, String theDisplay, String theValueSetUrl){
        
        System.out.println("theCodeSystem = " + theCodeSystem);
        System.out.println("theCode = " + theCode);
        CodeValidationResult codeValidationResult2 = validationSupport.validateCode
        (validationSupportContext, new ConceptValidationOptions(), theCodeSystem, theCode, theDisplay, theValueSetUrl);
        if (codeValidationResult2 != null && codeValidationResult2.isOk()) {
            return codeValidationResult2.getDisplay();
        }
        return null;
    }
    
    public CodeValidationResult getValidationResult(String theCodeSystem, String theCode, String theDisplay, String theValueSetUrl){
        
        System.out.println("theCodeSystem = " + theCodeSystem);
        System.out.println("theCode = " + theCode);
        CodeValidationResult codeValidationResult2 = validationSupport.validateCode
        (validationSupportContext, new ConceptValidationOptions().setInferSystem(true), theCodeSystem, theCode, theDisplay, theValueSetUrl);
        /*ValueSetExpansionOptions vse = new ValueSetExpansionOptions();
        vse.setFilter(theCode);
        IValidationSupport.ValueSetExpansionOutcome vsExp = validationSupport.expandValueSet(validationSupportContext,vse, theValueSetUrl);
         */
        if (codeValidationResult2 != null) {
          return codeValidationResult2;
        }
        return null;
    }
    
    
    public boolean isValueSetSupported(String theValueSetUrl){
        
      return validationSupport.isValueSetSupported(validationSupportContext, theValueSetUrl);
    }
    
}
