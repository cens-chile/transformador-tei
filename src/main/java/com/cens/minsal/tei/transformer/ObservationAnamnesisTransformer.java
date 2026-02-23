/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSIndiceComorbilidadValuexEnum;
import com.fasterxml.jackson.databind.JsonNode;
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
public class ObservationAnamnesisTransformer {

    static final String profile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ObservationAnamnesisLE";
    ValueSetValidatorService validator;

    public ObservationAnamnesisTransformer(ValueSetValidatorService validator){
        this.validator = validator;
    }

    public Observation  transform(JsonNode node, OperationOutcome oo){
        Observation obs = new Observation();

        obs.getMeta().addProfile(profile);
        //obs.getMeta().setLastUpdated(new Date());

        obs.setStatus(Observation.ObservationStatus.REGISTERED);
            String cs = "http://snomed.info/sct"; //"https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSTipoObservacionMinsal";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/TipoDeObservacion";
            String code = "84100007";
            Coding cod = new Coding(cs, code, "historia clínica");
            CodeableConcept cc = new CodeableConcept(cod);
            cc.setText(HapiFhirUtils.readStringValueFromJsonNode("Anámnesis", node));
            obs.setCode(cc);

        if(node.has("resultadoExamen")){
            StringType resExamen = new StringType(HapiFhirUtils.readStringValueFromJsonNode("resultadoExamen",node));
            obs.setValue(resExamen);
        }

        if(node.has("fechaExamen")){
            try {
                String fechaExamen = HapiFhirUtils.readDateTimeValueFromJsonNode("fechaExamen", node);
                DateTimeType dtt = new DateTimeType(fechaExamen);
                obs.setEffective(dtt);
            }catch (Exception e){
                HapiFhirUtils.addErrorIssue("Anamnesis.fechaExamen","Error al leer la fecha del examen en Anamnesis",oo);
            }
        }
        return obs;
    }
    
}
