/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSMessageHeaderEventEnum;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;

import java.util.Date;
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
        int i=0;
        for(JsonNode aiNode: alergias){
            AllergyIntolerance ai = new AllergyIntolerance();
            ai.getMeta().addProfile(profile);
            Coding codingFirstRep = ai.getCode().getCodingFirstRep();
            String code = aiNode.get("codigoSustancia").toString();
            codingFirstRep.setSystem(HapiFhirUtils.snomdeSystem);
            if(code!=null){
                codingFirstRep.setCode(code);
                if(aiNode.has("descripcionSustancia")){
                    String desc =HapiFhirUtils.readStringValueFromJsonNode("descripcionSustancia",aiNode);
                    ai.getCode().setText(desc);
                }
                if(aiNode.has("descripcionAlergia")){
                    String desc =HapiFhirUtils.readStringValueFromJsonNode("descripcionAlergia",aiNode);
                    ai.setText(new Narrative());
                }

            }
            else
                HapiFhirUtils.addNotFoundIssue("codigoSustancia", oo);
            
            String nombre = aiNode.get("nombre").toString();
            if(nombre!=null)
                codingFirstRep.setDisplay(nombre);

            alleIn.add(ai);
            i++;
        }
        
        
        return alleIn;
        
    }
    
}
