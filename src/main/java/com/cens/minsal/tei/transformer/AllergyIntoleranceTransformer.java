/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;

import java.util.List;

import org.hl7.fhir.r4.model.*;

import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class AllergyIntoleranceTransformer {
    private static final String profile = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/AllergyIntoleranceIniciarLE";
    ValueSetValidatorService validator;

    public AllergyIntoleranceTransformer(ValueSetValidatorService validator) {
        this.validator = validator;
    }
    public List<AllergyIntolerance> transform(JsonNode alergias, OperationOutcome oo){
        List<AllergyIntolerance> alleIn=new ArrayList();
        if(!alergias.isArray()){
            HapiFhirUtils.addErrorIssue("alergias", "alergias debe ser un arreglo.", oo);
            return null;
        }
        int i=0;
        for(JsonNode aiNode: alergias){
            AllergyIntolerance ai = new AllergyIntolerance();
            ai.getMeta().addProfile(profile);
            CodeableConcept c = new CodeableConcept();
            c.getCodingFirstRep().setCode("active");
            c.getCodingFirstRep().setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical");
            ai.setClinicalStatus(c);
            Coding codingFirstRep = ai.getCode().getCodingFirstRep();
            String code = HapiFhirUtils.readStringValueFromJsonNode("codigoSustancia", aiNode);
            codingFirstRep.setSystem(HapiFhirUtils.snomdeSystem);
            if(code!=null){
                codingFirstRep.setCode(code);
                if(aiNode.has("descripcionAlergia")){
                    String desc =HapiFhirUtils.readStringValueFromJsonNode("descripcionAlergia",aiNode);
                    ai.getCode().setText(desc);
                }else{
                    HapiFhirUtils.addNotFoundIssue("alergias["+i+"].descripcionAlergia", oo);
                }
                
                String glosa = HapiFhirUtils.readStringValueFromJsonNode("glosaSustancia", aiNode);
                if(glosa!=null)
                    codingFirstRep.setDisplay(glosa);

            }
            else
                HapiFhirUtils.addNotFoundIssue("alergias["+i+"].codigoSustancia", oo);
            
            

            alleIn.add(ai);
            i++;
        }
        
        
        return alleIn;
        
    }
    
}
