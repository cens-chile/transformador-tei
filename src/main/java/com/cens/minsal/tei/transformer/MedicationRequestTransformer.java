/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSIndiceComorbilidadValuexEnum;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.json.Json;
import org.apache.jena.web.HttpSC;
import org.hl7.fhir.r4.model.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
public class MedicationRequestTransformer {

    ValueSetValidatorService validator;

    public MedicationRequestTransformer(ValueSetValidatorService validator){
        this.validator = validator;
    }
    public MedicationRequest transform(JsonNode node, OperationOutcome oo) {
        MedicationRequest mr = new MedicationRequest();

        mr.getMeta().addProfile("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/MedicationRequestLE");
        //mr.getMeta().setLastUpdated(new Date());

        mr.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
        mr.setIntent(MedicationRequest.MedicationRequestIntent.PLAN);

        String cs = "http://snomed.info/sct";
        String vs = "http://hl7.org/fhir/ValueSet/medication-codes";
        String codMed = HapiFhirUtils.readStringValueFromJsonNode("medicamento",node);
        String nombreMed= HapiFhirUtils.readStringValueFromJsonNode("nombreMedicamento",node);
        String valido = validator.validateCode(cs,codMed,"",vs);
            Coding cod = new Coding(vs,codMed,nombreMed);
            CodeableConcept cc = new CodeableConcept(cod);
            mr.setMedication(cc);


        if (node.has("indicacion")) {
            List<Annotation> theNote = new ArrayList<>();
            Annotation anot= new Annotation(new MarkdownType(HapiFhirUtils.readStringValueFromJsonNode("indicacion", node)));
            theNote.add(anot);
            mr.setNote(theNote);
        }else HapiFhirUtils.addNotFoundIssue("SolicitudMedicamento.indicacion", oo);

        return mr;
    }


}
