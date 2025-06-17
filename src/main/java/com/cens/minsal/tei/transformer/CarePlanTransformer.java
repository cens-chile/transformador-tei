/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
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

        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("planDeAtencion no está presente o no es un objeto JSON.");
        }

        CarePlan carePlan = new CarePlan();

        // ID [2]
        String id = HapiFhirUtils.readStringValueFromJsonNode("id", node);
        if (id != null) {
            carePlan.setId(id);
        }

        // Meta.profile [2]
        carePlan.getMeta().addProfile("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/CarePlanAtenderLE");
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        carePlan.setIntent(CarePlan.CarePlanIntent.PLAN);
        carePlan.getMeta().setLastUpdated(new Date());

        // status [1..1] - binding required, debe ser "active"
        /* NO SE REQUIERE EL ESTADO NI LA INTENCION .  ESTÁTICOS

        String estado = HapiFhirUtils.readStringValueFromJsonNode("estado", node);
        if ("activo".equalsIgnoreCase(estado)) {

        } else {
            HapiFhirUtils.addInvalidIssue("El estado debe ser 'activo' según el perfil.",oo);
        }
         String intencion = HapiFhirUtils.readStringValueFromJsonNode("intencion", node);
        if ("plan".equalsIgnoreCase(intencion)) {
        } else {
            HapiFhirUtils.addNotFoundIssue("La intención debe ser 'plan' según el perfil.",oo);
        }*/

        // intent [1..1] - binding required, debe ser "plan"


        // title (extensión) - descripción del plan
        String descripcion = HapiFhirUtils.readStringValueFromJsonNode("descripcion", node);
        if (descripcion != null) {
            carePlan.setDescription(descripcion);
        } else{
            HapiFhirUtils.addNotFoundIssue("Descripción(Indicaciones medicas) del Plan de atención no debe ser vacío",oo);
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


        return carePlan;
    }
}

