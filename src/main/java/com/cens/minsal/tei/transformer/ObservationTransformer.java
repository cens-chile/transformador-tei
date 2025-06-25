/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSIndiceComorbilidadValuexEnum;
import com.fasterxml.jackson.databind.JsonNode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hl7.fhir.r4.model.*;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
public class ObservationTransformer {
    
    static final String profile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ObservationIndiceComorbilidadLE";
    static final String discapacidadProfile = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ObservationDiscapacidadLE";
    static final String cuidadorProfile = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ObservationIniciarCuidadorLE";
    static final String resultadoExProfile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ObservationResultadoExamen";
    ValueSetValidatorService validator;

    public ObservationTransformer(ValueSetValidatorService validator){
        this.validator = validator;
    }
    
    
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
    
    public static Observation buildDiscapacidad(boolean discapacidad){
        
        
        Observation ob = new Observation();
        ob.getMeta().addProfile(discapacidadProfile);
        
        ob.setStatus(Observation.ObservationStatus.FINAL);
        
        ob.getCode().addCoding((new Coding()).setSystem("http://loinc.org").setCode("101720-1"));
        
        ob.setValue(new BooleanType(discapacidad));

        return ob;
    }
    
    public static Observation buildCuidador(boolean cuidador){
        
        Observation ob = new Observation();
        ob.getMeta().addProfile(cuidadorProfile);
        
        ob.setStatus(Observation.ObservationStatus.FINAL);
        
        ob.getCode().addCoding((new Coding()).setSystem("http://loinc.org").setCode("95385-1"));
        
        ob.setValue(new BooleanType(cuidador));

        return ob;
    }
    
    public List<Observation> buildResultadoExamen(JsonNode resultadoExs, OperationOutcome oo){
        List<Observation> obs = new ArrayList();
        int i=0;
        for(JsonNode resultadoEx: resultadoExs){
        
            Observation ob = new Observation();

            IdType obID = IdType.newRandomUuid();
            ob.setId(obID.getIdPart());
            ob.getMeta().addProfile(resultadoExProfile);

            ob.setStatus(Observation.ObservationStatus.REGISTERED);

            ob.getCategoryFirstRep().
                addCoding(
                new Coding("http://terminology.hl7.org/CodeSystem/observation-category",
                "laboratory",""));


            String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", resultadoEx);
            if(codigo!=null){
                String cs = HapiFhirUtils.loincSystem;
                String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/CodigoExamen";
                ob.getCode().getCodingFirstRep().setCode(codigo);
                ob.getCode().getCodingFirstRep().setSystem(cs);
                String valido = validator.validateCode(cs,codigo,"",vs);
                valido =  HapiFhirUtils.readStringValueFromJsonNode("examen", resultadoEx);
                ob.getCode().getCodingFirstRep().setDisplay(valido);
                ob.getCode().setText("exámenes");
            }
            else 
                HapiFhirUtils.addNotFoundIssue("resultadoExamenes["+i+"]"+".codigo", oo);
            
            try {
                Date date = HapiFhirUtils.readDateValueFromJsonNode("fechaExamen", resultadoEx);
                ob.setEffective(new DateTimeType(date));
            } catch (ParseException ex) {
                Logger.getLogger(ObservationTransformer.class.getName()).log(Level.SEVERE, null, ex);
                HapiFhirUtils.addErrorIssue("resultadoExamenes["+i+"]"+".fechaExamen", ex.getMessage(), oo);
            }
            
            String resultado = HapiFhirUtils.readStringValueFromJsonNode("resultado", resultadoEx);
            if(resultado!=null)
                ob.setValue(new StringType(resultado));
            else
                HapiFhirUtils.addNotFoundIssue("resultadoExamenes["+i+"]"+".resultado", oo);
            
            obs.add(ob);
            i++;

        }
        
        return obs;
    }
    
}
