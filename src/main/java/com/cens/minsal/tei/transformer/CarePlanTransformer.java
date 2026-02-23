/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.json.Json;
import net.sourceforge.plantuml.tim.stdlib.Now;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 *
 * @author Juan F. <jfanasco@cens.cl>
 */
@Component
public class CarePlanTransformer {

    ValueSetValidatorService validator;

    public CarePlanTransformer(ValueSetValidatorService validator) {
        this.validator = validator;
    }


    public static CarePlan transform(JsonNode node, OperationOutcome oo) {

        boolean planValid = HapiFhirUtils.validateObjectInJsonNode("planDeAtencion", node,oo,true);
        CarePlan carePlan = new CarePlan();
        if(planValid) {

            // Meta.profile [2]
            carePlan.getMeta().addProfile("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/CarePlanAtenderLE");
            carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
            carePlan.setIntent(CarePlan.CarePlanIntent.PLAN);
            //carePlan.getMeta().setLastUpdated(new Date());

            // title (extensión) - descripción del plan
            String descripcion = HapiFhirUtils.readStringValueFromJsonNode("indicacionesMedicas", node);
            if (descripcion != null) {
                carePlan.setDescription(descripcion);
            } else {
                HapiFhirUtils.addNotFoundIssue("planDeAtencion.indicacionesMedicas(descripción) del Plan de atención" +
                        " no debe ser vacío", oo);
            }

            // requiereExamen (extensión booleana)
            Boolean requiereExamen = HapiFhirUtils.readBooleanValueFromJsonNode("requiereExamen", node);
            if (requiereExamen != null) {
                carePlan.addExtension(HapiFhirUtils.buildBooleanExt(
                        "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionSolicitudExamenes",
                        requiereExamen));
            } else {
                HapiFhirUtils.addNotFoundIssue("Solicitud de Exámen no puede ser vacío", oo);
            }

        } else{
            HapiFhirUtils.addNotFoundIssue("planDeAtencion",oo);
        }
        return carePlan;
    }
}

