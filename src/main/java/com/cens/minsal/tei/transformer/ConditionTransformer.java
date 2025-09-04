/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.stereotype.Component;

import java.util.Date;

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

        cond.getMeta().addProfile(profile);
        cond.getMeta().setLastUpdated(new Date());

        String codeCIE10 = HapiFhirUtils.readStringValueFromJsonNode("codeCIE10", node);
        if(codeCIE10 != null){
            Coding codingCIE10 = new Coding();
            codingCIE10.setSystem("http://hl7.org/fhir/sid/icd-10");
            codingCIE10.setCode(codeCIE10);
            String glosaCIE10 = HapiFhirUtils.readStringValueFromJsonNode("glosaCIE10", node);
            if(glosaCIE10 != null) {
                codingCIE10.setDisplay(glosaCIE10);
            }
            cond.getCode().addCoding(codingCIE10);
        }
        else
            HapiFhirUtils.addNotFoundIssue(parentPath+".codeCIE10", oo);

        String codeSNOMED = HapiFhirUtils.readStringValueFromJsonNode("codeSNOMED", node);
        if(codeSNOMED != null){
            Coding codingSNOMED = new Coding();
            codingSNOMED.setSystem("http://snomed.info/sct");
            codingSNOMED.setCode(codeSNOMED);
            String glosaSNOMED = HapiFhirUtils.readStringValueFromJsonNode("glosaSNOMED", node);
            if(glosaSNOMED != null) {
                codingSNOMED.setDisplay(glosaSNOMED);
            }
            cond.getCode().addCoding(codingSNOMED);
        }
        else
            HapiFhirUtils.addNotFoundIssue(parentPath+".codeSNOMED", oo);

        boolean valueSetSupported = validator.isValueSetSupported(codeVS);

        String text = HapiFhirUtils.readStringValueFromJsonNode("diagnosticoTexto", node);
        if(text!=null) {
            cond.getCode().setText(text);
        }
        
        
        String estadoDiagCode = HapiFhirUtils.readStringValueFromJsonNode("estadoDiagnostico", node);
        if(estadoDiagCode!=null){
            String clinicalStatusVS = "http://hl7.org/fhir/ValueSet/condition-clinical";
            String system = "http://terminology.hl7.org/CodeSystem/condition-clinical";
            String validateCode = validator.validateCode(system, estadoDiagCode, null, clinicalStatusVS);
            if(validateCode!=null){
                cond.getClinicalStatus().getCodingFirstRep().setCode(estadoDiagCode);
                cond.getClinicalStatus().getCodingFirstRep().setSystem(system);
                cond.getClinicalStatus().getCodingFirstRep().setDisplay(validateCode);
                cond.getClinicalStatus().setText(estadoDiagCode); // Esto debiera venir desde el JSON de entrada, distinto del JSON
            }else
                HapiFhirUtils.addErrorIssue("[estadoDiagnostico] code and system", "error al validar en el ValueSet", oo);
        }else
            HapiFhirUtils.addNotFoundIssue("estadoDiagnostico", oo);
        
        
        String clinicalVerStatus = HapiFhirUtils.readStringValueFromJsonNode("estadoVerificacion", node);
        if(clinicalVerStatus!=null){
            String verificationStatusVS = "http://hl7.org/fhir/ValueSet/condition-ver-status";
            String system = "http://terminology.hl7.org/CodeSystem/condition-ver-status";
            String validateCode = validator.validateCode(system, clinicalVerStatus, null, verificationStatusVS);
            if(validateCode!=null){
                cond.getVerificationStatus().getCodingFirstRep().setCode(clinicalVerStatus);
                cond.getVerificationStatus().getCodingFirstRep().setSystem(system);
                cond.getVerificationStatus().getCodingFirstRep().setDisplay(validateCode);
            }
            else
                HapiFhirUtils.addErrorIssue("code and system", "error al validar en el ValueSet", oo);
        }else
            HapiFhirUtils.addNotFoundIssue("estadoVerificacion", oo);
        
        String category = HapiFhirUtils.readStringValueFromJsonNode("categoria", node);
        if(category!=null){
            String categoryVS = "http://hl7.org/fhir/ValueSet/condition-category";
            String system = "http://terminology.hl7.org/CodeSystem/condition-category";
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
            String system = "http://snomed.info/sct";
            String validateCode = validator.validateCode(system, severity, "", categoryVS);
            //if(validateCode!=null){
            if(severity.equals("24484000") || severity.equals("6736007") || severity.equals("255604002")){
                cond.getSeverity().getCodingFirstRep().setCode(severity);
                cond.getSeverity().getCodingFirstRep().setSystem(system);
                cond.getSeverity().getCodingFirstRep().setDisplay(validateCode);
                }
            else{
                HapiFhirUtils.addNotFoundCodeIssue("diagnostico.severidad solo acepta los valores " +
                        "Severo=2448400 Moderado =6736007 leve=255604002 ", oo);
            }

        }
        return cond;
    }

}
