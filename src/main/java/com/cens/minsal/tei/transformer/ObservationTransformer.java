/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSIndiceComorbilidadValuexEnum;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
public class ObservationTransformer {
    
    static final String profile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ObservationIndiceComorbilidadLE";
    
    public static Observation buildIndiceComporbilidad(JsonNode indice, OperationOutcome oo){
        
        
        Observation ob = new Observation();
        ob.getMeta().addProfile(profile);
        
        ob.setStatus(Observation.ObservationStatus.FINAL);
        ob.getCategoryFirstRep().
            addCoding(
            new Coding("http://terminology.hl7.org/CodeSystem/observation-category",
            "survey",""));
        
        ob.getCode().addCoding((new Coding()).setCode("ECICEP")).setText("Indice Comorbilidad");
        
        
        VSIndiceComorbilidadValuexEnum fromCode = VSIndiceComorbilidadValuexEnum.fromCode(indice.asText());
        if(fromCode==null){
            HapiFhirUtils.addErrorIssue("indiceComorbilidad","código no encontrado", oo);
            return null;
        }
        ob.setValue((new CodeableConcept()).addCoding(fromCode.getCoding()));
        
        
        return ob;
    }
    
    public static Observation buildDiscapacidad(boolean discapacidad, OperationOutcome oo){
        
        
        Observation ob = new Observation();
        ob.getMeta().addProfile(profile);
        
        ob.setStatus(Observation.ObservationStatus.FINAL);
        ob.getCategoryFirstRep().
            addCoding(
            new Coding("http://terminology.hl7.org/CodeSystem/observation-category",
            "survey",""));
        
        ob.getCode().addCoding((new Coding()).setSystem("http://loinc.org").setCode("101720-1"));
        
        ob.setValue(new BooleanType(discapacidad));

        return ob;
    }
    
}
