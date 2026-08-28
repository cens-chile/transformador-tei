/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.Coding;
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

    private static final String PROBLEMA_GES_SYSTEM = "http://snomed.info/sct";
    
    private static final String profile =
            "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/QuestionnaireResponseIniciarLE";
    
    private static final String questionnaire =
            "https://interoperabilidad.minsal.cl/fhir/ig/tei/Questionnaire/MotivoDerivacion";
    
    public QuestionnaireResponse transform(JsonNode node, OperationOutcome oo){
        String mot = HapiFhirUtils.readStringValueFromJsonNode("motivoDerivacion", node);
        
        if(mot == null){
            HapiFhirUtils.addNotFoundIssue("motivoDerivacion", oo);
            return null;
        }
        
        QuestionnaireResponse quest = new QuestionnaireResponse();
        quest.getMeta().addProfile(profile);
        quest.setQuestionnaire(questionnaire);
        quest.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
        
        QuestionnaireResponse.QuestionnaireResponseItemComponent item = quest.getItemFirstRep();
        item.setLinkId("MotivoDerivacion");
        item.setText("Motivo de Derivación");
        item.getAnswerFirstRep().setValue(new StringType(mot));

        addGesItems(node, quest, oo);
        
        return quest;
    }

    private void addGesItems(JsonNode node, QuestionnaireResponse quest, OperationOutcome oo) {
        JsonNode solicitudIC = node.get("solicitudIC");
        if (solicitudIC == null || !solicitudIC.isObject()) {
            return;
        }

        Boolean correspondeGes = HapiFhirUtils.readBooleanValueFromJsonNode("correspondeGES", solicitudIC);
        if (!Boolean.TRUE.equals(correspondeGes)) {
            return;
        }

        JsonNode problemaGes = solicitudIC.get("problemaSaludGES");
        if (!HapiFhirUtils.validateObjectInJsonNode(
                "solicitudIC.problemaSaludGES", problemaGes, oo, true)) {
            return;
        }

        String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", problemaGes);
        if (codigo == null) {
            HapiFhirUtils.addNotFoundIssue("solicitudIC.problemaSaludGES.codigo", oo);
            return;
        }

        String glosa = HapiFhirUtils.readStringValueFromJsonNode("glosa", problemaGes);

        QuestionnaireResponse.QuestionnaireResponseItemComponent grupoGes = quest.addItem();
        grupoGes.setLinkId("GarantiaExplicitaenSalud-GES");
        grupoGes.setText("Garantía Explícita en Salud (GES)");

        QuestionnaireResponse.QuestionnaireResponseItemComponent problemaItem = grupoGes.addItem();
        problemaItem.setLinkId("ProblemadeSaludGES");
        problemaItem.setText("Problema de Salud GES");

        Coding coding = new Coding();
        coding.setSystem(PROBLEMA_GES_SYSTEM);
        coding.setCode(codigo);
        if (glosa != null) {
            coding.setDisplay(glosa);
        }
        problemaItem.getAnswerFirstRep().setValue(coding);

        String subProblema = HapiFhirUtils.readStringValueFromJsonNode(
                "subProblemaSaludGES", solicitudIC);
        if (subProblema != null) {
            QuestionnaireResponse.QuestionnaireResponseItemComponent subProblemaItem = grupoGes.addItem();
            subProblemaItem.setLinkId("SubProblemadeSaludGES");
            subProblemaItem.setText("SubProblema de Salud GES");
            subProblemaItem.getAnswerFirstRep().setValue(new StringType(subProblema));
        }
    }
}
