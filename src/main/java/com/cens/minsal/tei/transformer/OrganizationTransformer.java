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
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class OrganizationTransformer {
    
    static final String profile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/OrganizationLE";
    
    public static Organization transform(JsonNode node, OperationOutcome oo,String parentPath){
        
        
        Organization org = new Organization();
        org.getMeta().addProfile(profile);
        
        String nombre = HapiFhirUtils.readStringValueFromJsonNode("nombreLegal", node);
        System.out.println("+++++++++++++++++++++++++"+nombre);
        
        if(nombre!=null)
            org.setName(nombre);
        else 
            HapiFhirUtils.addNotFoundIssue(parentPath+"nombreLegal", oo);
        String codigoDEIS = HapiFhirUtils.readStringValueFromJsonNode("codigoDEIS", node);
        if(codigoDEIS!=null)
            org.getIdentifierFirstRep().setValue(codigoDEIS);
        else 
            HapiFhirUtils.addNotFoundIssue("establecimientoAPS->codigoDEIS", oo);
        
        
        return org;
    }
    
}
