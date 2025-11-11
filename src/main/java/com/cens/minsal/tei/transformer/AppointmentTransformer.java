package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class AppointmentTransformer {

    ValueSetValidatorService validator;

    public AppointmentTransformer(ValueSetValidatorService validator) {
        this.validator = validator;
    }

    public Appointment transform(JsonNode nodeOrigin, OperationOutcome oo) {
        Appointment appointment = new Appointment();
        appointment.getMeta().addProfile("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/AppointmentAgendarLE");

        JsonNode node = nodeOrigin.get("cita");

        if (node.has("id")) {
            IdType idType = new IdType();
            idType.setValue(HapiFhirUtils.readStringValueFromJsonNode("id", node));
            appointment.setId(idType);
        }

        // Identificador
        if (node.has("identificador")) {
            String identifString =HapiFhirUtils.readStringValueFromJsonNode("identificador", node);
            if(identifString!=null){
            Identifier identifier = new Identifier();
            identifier.setValue(identifString);
            appointment.addIdentifier(identifier);
            }else{
                HapiFhirUtils.addErrorIssue("cita.identificador","String vacío o no valido",oo);
            }

        } else {
            HapiFhirUtils.addNotFoundIssue("cita.identificador", oo);
        }



        //FechaCreacionCita

        appointment.setCreated(new Date());

        //MediodeContacto

        if (node.has("medioDeContacto")){
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSMediodeContacto";
            String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSMediodeContacto";
            String medioDeContacto = HapiFhirUtils.readStringValueFromJsonNode("medioDeContacto", node);
            String valido = validator.validateCode(cs,medioDeContacto,"",vs);
            Coding mcCod = new Coding(cs,medioDeContacto,valido);
            CodeableConcept cc = new CodeableConcept(mcCod);
            Extension mcExt = new Extension(
                    "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionMediodeContacto", cc);
            appointment.addExtension(mcExt);

        }else HapiFhirUtils.addNotFoundIssue("cita.medioDeContacto", oo);

        // Estado
        if (node.has("estado")) {
            String estado = node.get("estado").asText();
            try {
                appointment.setStatus(Appointment.AppointmentStatus.fromCode(estado));
                if(estado.equalsIgnoreCase("cancelled") || estado.equalsIgnoreCase("no-show")){
                    if(node.has("motivoCancelacionCita")){
                        String vs ="http://hl7.org/fhir/ValueSet/appointment-cancellation-reason";
                        String cs = "http://terminology.hl7.org/CodeSystem/appointment-cancellation-reason";
                        String code = HapiFhirUtils.readStringValueFromJsonNode("motivoCancelacionCita",node);
                        String valido = validator.validateCode(cs,code,"",vs);
                        if(valido != null && valido.equalsIgnoreCase("pat") || valido.equalsIgnoreCase("prov") || valido.equalsIgnoreCase("other")) {
                            Coding coding = new Coding(cs, estado, valido);
                            CodeableConcept cc = new CodeableConcept(coding);
                            appointment.setCancelationReason(cc);
                        }else HapiFhirUtils.addNotFoundCodeIssue("cita.motivoCancelacionCita",oo);
                    }else HapiFhirUtils.addNotFoundIssue("Cita.Estado cancelled o no-show implica que debe existir motivoCancelacionCita", oo);
                }
            } catch (Exception e) {
                HapiFhirUtils.addErrorIssue("Estado inválido: " + estado, "Cita.estado", oo);
            }
        } else {
            HapiFhirUtils.addNotFoundIssue("cita.estado", oo);
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
                Date start = HapiFhirUtils.readDateTimeValueFromJsonNode("fechaInicio", node, "yyyy-MM-dd HH:mm:ss");
                Date end = HapiFhirUtils.readDateTimeValueFromJsonNode("fechaFin", node, "yyyy-MM-dd HH:mm:ss");
                appointment.setStart(start);
                appointment.setEnd(end);
            } catch (ParseException e) {
                HapiFhirUtils.addErrorIssue("Error al parsear fechas de la cita", "fechaInicio/fechaFin", oo);
            }
        } else {
            HapiFhirUtils.addNotFoundIssue("fechaInicio o fechaFin", oo);
        }


        if(node.has("contactadoLE")){
            Extension contactadoLEExt = new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/Contactado");
            JsonNode contactadoLE = node.get("contactadoLE");
            if(contactadoLE.has("contactado")){
                Boolean contactadoB = HapiFhirUtils.readBooleanValueFromJsonNode("contactado", contactadoLE);
                Extension contactadoBExt = new Extension("Contactado",new BooleanType(contactadoB));
                if(!contactadoB){
                    if(contactadoLE.has("motivoNoContactabilidad")){
                        String mnc =  HapiFhirUtils.readStringValueFromJsonNode("motivoNoContactabilidad",contactadoLE);
                        String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSMotivoNoContactabilidad";
                        String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSMotivoNoContactabilidad";
                        String valido = validator.validateCode(cs,mnc,"",vs);
                        if(valido != null) {
                            Coding cod = new Coding(cs, mnc, valido);
                            CodeableConcept cc = new CodeableConcept(cod);
                            Extension mNCExt = new Extension("MotivoNoContactabilidad", cc);
                            contactadoLEExt.addExtension(mNCExt);
                        } else HapiFhirUtils.addNotFoundCodeIssue("Cita.ContactadoLE.motivoNoContactabilidad",oo);

                    }else HapiFhirUtils.addNotFoundIssue("Cita.ContactadoLE.motivoNoContactabilidad", oo);
                } else{
                    contactadoLEExt.addExtension(contactadoBExt);
                }
                appointment.addExtension(contactadoLEExt);

            } else HapiFhirUtils.addNotFoundIssue("Cita.ContactadoLE.contactado", oo);
        } else HapiFhirUtils.addNotFoundIssue("Cita.contactadoLE", oo);


        // Participantes

        if (node.has("estadoActorPaciente")) {
            String eap = HapiFhirUtils.readStringValueFromJsonNode("estadoActorPaciente", node);
            Reference pacienteRef = new Reference(
                    HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente",nodeOrigin));
            String vs ="http://hl7.org/fhir/ValueSet/resource-types";
            String cs = "http://hl7.org/fhir/resource-types";

            Coding coding = new Coding(cs, "Patient", "Patient");
            CodeableConcept cc = new CodeableConcept(coding);
            List<CodeableConcept> ccList = new ArrayList<>();
            ccList.add(cc);
            switch (eap) {
                case ("accepted"):
                    appointment.addParticipant(new Appointment.AppointmentParticipantComponent()
                            .setActor(pacienteRef.setType(coding.getCode()))
                            .setStatus(Appointment.ParticipationStatus.ACCEPTED));
                    break;
                case ("declined"):
                    appointment.addParticipant(new Appointment.AppointmentParticipantComponent()
                            .setActor(pacienteRef.setType(coding.getCode()))
                            .setStatus(Appointment.ParticipationStatus.DECLINED));
                    break;
                case ("tentative"):
                    appointment.addParticipant(new Appointment.AppointmentParticipantComponent()
                            .setActor(pacienteRef.setType(coding.getCode())).setStatus(Appointment.ParticipationStatus.TENTATIVE));
                    break;
                case ("needs-action"):
                    appointment.addParticipant(new Appointment.AppointmentParticipantComponent()
                            .setActor(pacienteRef.setType(coding.getCode())).setStatus(Appointment.ParticipationStatus.NEEDSACTION));
                    break;
                default:
                    HapiFhirUtils.addErrorIssue("Cita.estadoActorPaciente","valor no válido. Solo es válido: accepted declined tentative needs-action.", oo);
                    break;
            }
        } else HapiFhirUtils.addNotFoundIssue("Cita.estadoActorPaciente",oo);



        return appointment;
    }
}