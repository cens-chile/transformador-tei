/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Juan F. <jfanasco@cens.cl>
 */
@Component
public class ServiceRequestTransformer {
    
    private final String profileExamen = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ServiceRequestExamenLE";
    
    
    
    
    
    
    public ServiceRequest transform(String profile, JsonNode node, OperationOutcome oo) {
        ServiceRequest sr = new ServiceRequest();
        sr.getMeta().addProfile(profile);
        sr.getMeta().setLastUpdated(new Date());

        sr.setId(HapiFhirUtils.readStringValueFromJsonNode("ID", node));

        // Identificadores
        JsonNode identifiers = node.get("IdentificadorInterconsulta");
        if (identifiers != null && identifiers.isArray()) {
            for (JsonNode id : identifiers) {
                Identifier identifier = new Identifier();
                identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
                identifier.setValue(HapiFhirUtils.readStringValueFromJsonNode("valor", id));
                sr.addIdentifier(identifier);
            }
        }

        // Estado
        String estado = HapiFhirUtils.readStringValueFromJsonNode("Estado", node);
        if (estado != null) {
            sr.setStatus(ServiceRequest.ServiceRequestStatus.fromCode(estado));
        }

        // Intención
        String intencion = HapiFhirUtils.readStringValueFromJsonNode("Intencion", node);
        if (intencion != null) {
            sr.setIntent(ServiceRequest.ServiceRequestIntent.fromCode(intencion));
        }

        // Prioridad
        String prioridad = HapiFhirUtils.readStringValueFromJsonNode("Prioridad", node);
        if (prioridad != null) {
            sr.setPriority(ServiceRequest.ServiceRequestPriority.fromCode(prioridad));
        }

        // Sujeto, Encuentro y Solicitante
        sr.setSubject(new Reference(HapiFhirUtils.readStringValueFromJsonNode("Paciente", node)));
        sr.setEncounter(new Reference(HapiFhirUtils.readStringValueFromJsonNode("Encuentro", node)));
        sr.setRequester(new Reference(HapiFhirUtils.readStringValueFromJsonNode("Solicitante", node)));

        // Fecha de solicitud
        try {
            Date authoredOn = HapiFhirUtils.readDateTimeValueFromJsonNode("fechaSolicitudIC", node, "dd-MM-yyyy HH:mm:ss");
            if (authoredOn != null) {
                sr.setAuthoredOn(authoredOn);
            }
        } catch (ParseException e) {
            HapiFhirUtils.addNotFoundIssue("fechaSolicitudIC inválida", oo);
        }

        // Código (servicio requerido)
        JsonNode codigo = node.get("CodigoSolicitud");
        if (codigo != null) {
            sr.setCode(HapiFhirUtils.buildCodeableConceptFromJson(codigo));
        }

        // Category (Modalidad de atención)
        JsonNode modalidad = node.get("ModalidadAtencion");
        if (modalidad != null) {
            sr.addCategory(HapiFhirUtils.buildCodeableConceptFromJson(modalidad));
        }

        // ReasonCode
        JsonNode derivadoPara = node.get("derivadoPara");
        if (derivadoPara != null) {
            sr.addReasonCode(HapiFhirUtils.buildCodeableConceptFromJson(derivadoPara));
        }

        // SupportingInformation
        JsonNode infoAdicional = node.get("InformacionAdicional");
        if (infoAdicional != null && infoAdicional.isArray()) {
            for (JsonNode item : infoAdicional) {
                item.fields().forEachRemaining(field -> {
                    String ref = field.getValue().asText();
                    if (ref != null && !ref.isBlank()) {
                        sr.addSupportingInfo(new Reference(ref));
                    }
                });
            }
        }

        // Notas como extensiones traducidas
        addStringExtension(sr, "fundamentoPriorizacion", node);
        addBooleanExtension(sr, "requiereExamen", node);
        addBooleanExtension(sr, "atencionPreferente", node);
        addBooleanExtension(sr, "resolutividadAPS", node);

        // Otros codificables
        addCodableExtension(sr, "estadoInterconsultaCodigo", node);
        addCodableExtension(sr, "origenInterconsulta", node);
        addCodableExtension(sr, "especialidadMedicaDestinoCodigo", node);
        addCodableExtension(sr, "ServicioRequerido", node);
        addCodableExtension(sr, "DestinoReferencia", node);

        // Ejecutante y especialista
        String ejecutor = HapiFhirUtils.readStringValueFromJsonNode("EjecutorSolicitud", node);
        if (ejecutor != null) {
            sr.addPerformer(new Reference(ejecutor));
        }

        String resolutor = HapiFhirUtils.readStringValueFromJsonNode("EspecialistaResolutor", node);
        if (resolutor != null) {
            sr.addPerformer(new Reference(resolutor));
        }

        return sr;
    }

    private void addStringExtension(ServiceRequest sr, String field, JsonNode node) {
        String val = HapiFhirUtils.readStringValueFromJsonNode(field, node);
        if (val != null) {
            sr.addExtension(new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition-ServiceRequestLE#" + field,
                    new StringType(val)));
        }
    }

    private void addBooleanExtension(ServiceRequest sr, String field, JsonNode node) {
        Boolean val = HapiFhirUtils.readBooleanValueFromJsonNode(field, node);
        if (val != null) {
            sr.addExtension(new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition-ServiceRequestLE#" + field,
                    new BooleanType(val)));
        }
    }

    private void addCodableExtension(ServiceRequest sr, String field, JsonNode node) {
        JsonNode val = node.get(field);
        if (val != null) {
            CodeableConcept concept = HapiFhirUtils.buildCodeableConceptFromJson(val);
            sr.addExtension(new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition-ServiceRequestLE#" + field,
                    concept));
        }
    }


    public List<ServiceRequest> buildSolicitudExamen(JsonNode solicitudes, OperationOutcome oo){
        JsonNode solicitudExamenNode = solicitudes.get("solicitudExamen");
        if(solicitudExamenNode==null)
            return null;
        
        List<ServiceRequest> sols= new ArrayList();
        int i=0;
        for(JsonNode node : solicitudExamenNode){
            ServiceRequest ser = new ServiceRequest();
            ser.getMeta().addProfile(profileExamen);

            try {
                Date fechaSolicitud = HapiFhirUtils.readDateValueFromJsonNode("fechaSolicitud", node);
                ser.setAuthoredOn(fechaSolicitud);
            } catch (ParseException ex) {
                Logger.getLogger(ServiceRequestTransformer.class.getName()).log(Level.SEVERE, null, ex);
                HapiFhirUtils.addErrorIssue("solicitudExamen["+i+"].fechaSolicitud", ex.getMessage(), oo);
            }

            ser.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
            ser.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

            String razonSolicitud = HapiFhirUtils.readStringValueFromJsonNode("razonSolicitud", node);
            if(razonSolicitud!=null){
                ser.getReasonCodeFirstRep().setText(razonSolicitud);
            }else
                HapiFhirUtils.addNotFoundIssue("solicitudExamen["+i+"].razonSolicitud", oo);

            String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigoExamen", node);

            CodeableConcept code = ser.getCode();

            Coding coding = code.getCodingFirstRep();

            if(codigo!=null){
                coding.setSystem(HapiFhirUtils.loincSystem);
                coding.setCode(codigo);
            }

            String examenSolicitado = HapiFhirUtils.readStringValueFromJsonNode("examenSolicitado", node);
            if(examenSolicitado!=null){
                code.setText(examenSolicitado);
            }else
               HapiFhirUtils.addNotFoundIssue("solicitudExamen["+i+"].examenSolicitado", oo);
            sols.add(ser);
        }
        return sols;
        
    }
    
}

