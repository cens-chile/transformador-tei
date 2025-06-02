/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
public class ConditionTransformer {
    
    private static final String profile= "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ConditionDiagnosticoLE";
    private static final String codeVS= "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSTerminologiasDiag";
    ValueSetValidatorService validator;

    public ConditionTransformer(ValueSetValidatorService validator) {
        this.validator = validator;
    }

    public Condition transform(JsonNode node, OperationOutcome oo,String parentPath){
        Condition cond = new Condition();
        
        String code = HapiFhirUtils.readStringValueFromJsonNode("code", node);
        if(code!=null)
            cond.getCode().getCodingFirstRep().setCode(code);
        else 
            HapiFhirUtils.addNotFoundIssue(parentPath+".code", oo);
        
        
        String system = HapiFhirUtils.readStringValueFromJsonNode("system", node);
        if(system!=null)
            cond.getCode().getCodingFirstRep().setSystem(system);
        else 
            HapiFhirUtils.addNotFoundIssue(parentPath+".system", oo);
        
        boolean valueSetSupported = validator.isValueSetSupported(codeVS);
        System.out.println("valueSetSupported = " + valueSetSupported);
        /*if(valueSetSupported){
            String validateCode = validator.validateCode(system, code, null, codeVS);
            if(validateCode==null)
                HapiFhirUtils.addErrorIssue("code and system", "error al validar en el ValueSet", oo);
        }*/
        
        
        String display = HapiFhirUtils.readStringValueFromJsonNode("glosa", node);
        if(display!=null)
            cond.getCode().getCodingFirstRep().setDisplay(display);
        
        String text = HapiFhirUtils.readStringValueFromJsonNode("text", node);
        if(text!=null)
            cond.getCode().setText(text);
        
        
        
        String estadoDiagCode = HapiFhirUtils.readStringValueFromJsonNode("estadoDiagnostico", node);
        if(estadoDiagCode!=null){
            String clinicalStatusVS = "http://hl7.org/fhir/ValueSet/condition-clinical";
            system = "http://terminology.hl7.org/CodeSystem/condition-clinical";
            String validateCode = validator.validateCode(system, estadoDiagCode, null, clinicalStatusVS);
            if(validateCode!=null){
                cond.getClinicalStatus().getCodingFirstRep().setCode(estadoDiagCode);
                cond.getClinicalStatus().getCodingFirstRep().setSystem(system);
                cond.getClinicalStatus().getCodingFirstRep().setDisplay(validateCode);
            }else
                HapiFhirUtils.addErrorIssue("[estadoDiagnostico] code and system", "error al validar en el ValueSet", oo);
        }else
            HapiFhirUtils.addNotFoundIssue("estadoDiagnostico", oo);
        
        
        String clinicalVerStatus = HapiFhirUtils.readStringValueFromJsonNode("estadoVerificacion", node);
        if(clinicalVerStatus!=null){
            String verificationStatusVS = "http://hl7.org/fhir/ValueSet/condition-ver-status";
            system = "http://terminology.hl7.org/CodeSystem/condition-ver-status";
            String validateCode = validator.validateCode(system, clinicalVerStatus, null, verificationStatusVS);
            if(validateCode!=null){
                cond.getClinicalStatus().getCodingFirstRep().setCode(clinicalVerStatus);
                cond.getClinicalStatus().getCodingFirstRep().setSystem(system);
                cond.getClinicalStatus().getCodingFirstRep().setDisplay(validateCode);
            }
            else
                HapiFhirUtils.addErrorIssue("code and system", "error al validar en el ValueSet", oo);
        }else
            HapiFhirUtils.addNotFoundIssue("estadoVerificacion", oo);
        
        String category = HapiFhirUtils.readStringValueFromJsonNode("categoria", node);
        if(category!=null){
            String categoryVS = "http://hl7.org/fhir/ValueSet/condition-category";
            system = "http://terminology.hl7.org/CodeSystem/condition-category";
            String validateCode = validator.validateCode(system, category, null, categoryVS);
            if(validateCode!=null){
                cond.getCategoryFirstRep().getCodingFirstRep().setCode(category);
                cond.getCategoryFirstRep().getCodingFirstRep().setSystem(system);
                cond.getCategoryFirstRep().getCodingFirstRep().setDisplay(validateCode);
            }else
                HapiFhirUtils.addErrorIssue("code and system", "error al validar en el ValueSet", oo);
        }else
            HapiFhirUtils.addNotFoundIssue("estadoVerificacion", oo);
        
        
        String severity = HapiFhirUtils.readStringValueFromJsonNode("severidad", node);
        if(severity!=null){
            String categoryVS = "http://hl7.org/fhir/ValueSet/condition-severity";
            system = "http://snomed.info/sct";
            String validateCode = validator.validateCode(system, clinicalVerStatus, null, categoryVS);
            if(validateCode!=null){
                cond.getSeverity().getCodingFirstRep().setCode(severity);
                cond.getSeverity().getCodingFirstRep().setSystem(system);
                cond.getSeverity().getCodingFirstRep().setDisplay(validateCode);
               
            }else
                HapiFhirUtils.addErrorIssue("code and system", "error al validar en el ValueSet", oo);
        }
        
        
        return cond;
    }

}
