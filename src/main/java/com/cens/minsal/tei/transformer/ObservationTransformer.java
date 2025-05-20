/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSIndiceComorbilidadValuexEnum;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
public class ObservationTransformer {
    
    static final String profile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ObservationIndiceComorbilidadLE";
    
    public static Observation buildIndiceComporbilidad(JsonNode indice){
        
        
        Observation ob = new Observation();
        ob.getMeta().addProfile(profile);
        
        ob.setStatus(Observation.ObservationStatus.FINAL);
        ob.getCategoryFirstRep().
            addCoding(
            new Coding("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ObservationIndiceComorbilidadLE",
            "survey",""));
        
        ob.getCode().addCoding((new Coding()).setCode("ECICEP")).setText("Indice Comorbilidad");
        
        
        VSIndiceComorbilidadValuexEnum fromCode = VSIndiceComorbilidadValuexEnum.fromCode(indice.asText());
        
        ob.setValue((new CodeableConcept()).addCoding(fromCode.getCoding()));
        
        
        return ob;
    }
    
}
