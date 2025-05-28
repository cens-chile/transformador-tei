/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

/**
 *
 * @author Juan F. <jfanasco@cens.cl>
 */
@Component
public class CarePlanTransformer {
    private static final String PROFILE_URL = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/CarePlanAtenderLE";

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

        // status [1..1] - binding required, debe ser "active"
        String estado = HapiFhirUtils.readStringValueFromJsonNode("estado", node);
        if ("activo".equalsIgnoreCase(estado)) {
            carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        } else {
            HapiFhirUtils.addInvalidIssue("El estado debe ser 'activo' según el perfil.",oo);
        }

        // intent [1..1] - binding required, debe ser "plan"
        String intencion = HapiFhirUtils.readStringValueFromJsonNode("intencion", node);
        if ("plan".equalsIgnoreCase(intencion)) {
            carePlan.setIntent(CarePlan.CarePlanIntent.PLAN);
        } else {
            HapiFhirUtils.addNotFoundIssue("La intención debe ser 'plan' según el perfil.",oo);
        }

        // title (extensión) - descripción del plan
        String descripcion = HapiFhirUtils.readStringValueFromJsonNode("descripcion", node);
        if (descripcion != null) {
            carePlan.addExtension(HapiFhirUtils.buildExtension(
                    "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ext-careplan-titulo",
                    new StringType(descripcion)
            ));
        } else{
            HapiFhirUtils.addNotFoundIssue("Descripción del Plan de atención no debe ser vacío",oo);
        }

        // requiereExamen (extensión booleana)
        Boolean requiereExamen = HapiFhirUtils.readBooleanValueFromJsonNode("requiereExamen", node);
        if (requiereExamen != null) {
            carePlan.addExtension(HapiFhirUtils.buildBooleanExt(
                    "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ext-careplan-requiere-examen",
                    requiereExamen
            ));
        } else {
            HapiFhirUtils.addNotFoundIssue("Solicitud de Exámen no puede ser vacío", oo);
        }

        // subject (paciente) [1..1]
        JsonNode pacienteNode = node.get("paciente");
        if (pacienteNode != null && pacienteNode.has("referenciaPaciente")) {
            String ref = pacienteNode.get("referenciaPaciente").asText();
            carePlan.setSubject(new Reference(ref));
        }else { HapiFhirUtils.addNotFoundIssue("Campo Paciente no puede ser nulo",oo);}

        // encounter [0..1]
        JsonNode encuentroNode = node.get("encuentro");
        if (encuentroNode != null && encuentroNode.has("referenciaEncuentro")) {
            String ref = encuentroNode.get("referenciaEncuentro").asText();
            carePlan.setEncounter(new Reference(ref));
        }

        // supportingInfo - recetas (MedicationRequest)
        JsonNode recetasNode = node.get("recetas");
        if (recetasNode != null && recetasNode.isArray()) {
            for (JsonNode receta : recetasNode) {
                if (receta.has("referenciaReceta")) {
                    String ref = receta.get("referenciaReceta").asText();
                    carePlan.addSupportingInfo(new Reference(ref));
                }
            }
        }

        // activity - solicitudes de atención (ServiceRequest)
        JsonNode solicitudesNode = node.get("SolicitudesDeAtencion");
        if (solicitudesNode != null && solicitudesNode.isArray()) {
            for (JsonNode solicitud : solicitudesNode) {
                if (solicitud.has("referenciaSolicitudDeAtencion")) {
                    String ref = solicitud.get("referenciaSolicitudDeAtencion").asText();
                    CarePlan.CarePlanActivityComponent actividad = new CarePlan.CarePlanActivityComponent();
                    actividad.setReference(new Reference(ref));
                    carePlan.addActivity(actividad);
                }
            }
        }

        return carePlan;
    }
}

