/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class QuestionnaireResponseTransformer {
    
    private static String profile = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/QuestionnaireResponseIniciarLE";
    
    public QuestionnaireResponse transform(JsonNode node, OperationOutcome oo){
        String mot = HapiFhirUtils.readStringValueFromJsonNode("motivoDerivacion", node);
        if(mot==null){
            HapiFhirUtils.addNotFoundIssue("motivoDerivacion", oo);
            return null;
        }
        QuestionnaireResponse quest = new QuestionnaireResponse();
        quest.getMeta().addProfile(profile);
        quest.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
        
        QuestionnaireResponse.QuestionnaireResponseItemComponent item = quest.getItemFirstRep();
        item.setLinkId("MotivoDerivacion");
        item.setText("Motivo Derivación");
        
        
        item.getAnswerFirstRep().setValue(new StringType(mot));
        
        
        return quest;
    }
    
}
