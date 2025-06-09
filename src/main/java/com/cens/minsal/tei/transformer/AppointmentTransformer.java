package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class AppointmentTransformer {

    public Appointment transform(JsonNode node, OperationOutcome oo) {
        Appointment appointment = new Appointment();
        appointment.getMeta().addProfile("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/AppointmentLE");

        // Identificador
        if (node.has("identificador")) {
            Identifier identifier = new Identifier();
            identifier.setValue(node.get("identificador").asText());
            appointment.addIdentifier(identifier);
        } else {
            HapiFhirUtils.addNotFoundIssue("identificador", oo);
        }

        // Estado
        if (node.has("estado")) {
            String estado = node.get("estado").asText();
            try {
                appointment.setStatus(Appointment.AppointmentStatus.fromCode(estado));
            } catch (Exception e) {
                HapiFhirUtils.addErrorIssue("Estado inválido: " + estado, "estado", oo);
            }
        } else {
            HapiFhirUtils.addNotFoundIssue("estado", oo);
        }

        // Motivo
        if (node.has("motivo")) {
            JsonNode motivo = node.get("motivo");
            CodeableConcept reason = new CodeableConcept();
            if (motivo.has("codigo")) {
                reason.addCoding(new Coding()
                        .setCode(motivo.get("codigo").asText())
                        .setSystem(motivo.has("sistema") ? motivo.get("sistema").asText() : "http://snomed.info/sct")
                        .setDisplay(motivo.has("glosa") ? motivo.get("glosa").asText() : null));
                appointment.addReasonCode(reason);
            }
        }

        // Fechas
        if (node.has("fechaInicio") && node.has("fechaFin")) {
            try {
                Date start = HapiFhirUtils.readDateTimeValueFromJsonNode("fechaInicio", node, "dd-MM-yyyy HH:mm:ss");
                Date end = HapiFhirUtils.readDateTimeValueFromJsonNode("fechaFin", node, "dd-MM-yyyy HH:mm:ss");
                appointment.setStart(start);
                appointment.setEnd(end);
            } catch (ParseException e) {
                HapiFhirUtils.addErrorIssue("Error al parsear fechas de la cita", "fechaInicio/fechaFin", oo);
            }
        } else {
            HapiFhirUtils.addNotFoundIssue("fechaInicio o fechaFin", oo);
        }

        // Duración
        if (node.has("duracion")) {
            appointment.setMinutesDuration(node.get("duracion").asInt());
        }

        // Participantes
        if (node.has("participantes")) {
            for (JsonNode participante : node.get("participantes")) {
                AppointmentParticipantComponent participant = new AppointmentParticipantComponent();

                if (participante.has("rol")) {
                    participant.addType(new CodeableConcept().addCoding(
                            new Coding()
                                    .setCode(participante.get("rol").asText())
                                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType")));
                }

                if (participante.has("individuo")) {
                    participant.setActor(new Reference(participante.get("individuo").asText()));
                }

                participant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
                appointment.addParticipant(participant);
            }
        }

        return appointment;
    }
}