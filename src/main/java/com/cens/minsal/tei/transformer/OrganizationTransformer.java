/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSIndiceComorbilidadValuexEnum;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.stereotype.Component;

import javax.xml.validation.Validator;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class OrganizationTransformer {

    ValueSetValidatorService validator;

    public OrganizationTransformer(ValueSetValidatorService validator){
        this.validator = validator;
    }
    
    static final String profile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/OrganizationLE";
    
    public Organization transform(JsonNode node, OperationOutcome oo,String parentPath){
        
        
        Organization org = new Organization();
        org.getMeta().addProfile(profile);
        
        String nombre = HapiFhirUtils.readStringValueFromJsonNode("nombreLegal", node);

        if(nombre!=null)
            org.setName(nombre);
        else 
            HapiFhirUtils.addNotFoundIssue(parentPath+"nombreLegal", oo);
        String codigoDEIS = HapiFhirUtils.readStringValueFromJsonNode("codigoDEIS", node);
        if(codigoDEIS!=null)
            org.getIdentifierFirstRep().setValue(codigoDEIS);
        else 
            HapiFhirUtils.addNotFoundIssue("establecimientoAPS->codigoDEIS", oo);

        String csDest = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEstablecimientoDestino";
        String vsDest = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEstablecimientoDestino";
        String resValidacionDest = validator.validateCode(csDest, codigoDEIS,"",vsDest);

        String csOri = "";
        String vsOri = "";
        String resValidacionOri = validator.validateCode(csDest, codigoDEIS,"",vsDest);

        if (resValidacionDest == null){
            HapiFhirUtils.addErrorIssue(codigoDEIS,"CodigoDEIS no valido", oo);
        }
        
        return org;
    }
    
}
