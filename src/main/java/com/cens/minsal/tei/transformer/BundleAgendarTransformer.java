package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSModalidadAtencionEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class BundleAgendarTransformer {

    private final FhirServerConfig fhirServerConfig;
    private static final String bundleProfile = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/BundleAgendarLE";

    private final MessageHeaderTransformer messageHeaderTransformer;
    private final PractitionerTransformer practitionerTransformer;
    private final PractitionerRoleTransformer practitionerRoleTransformer;
    private final PatientTransformer patientTransformer;
    private final OrganizationTransformer organizationTransformer;
    private final AppointmentTransformer appointmentTransformer;
    private final ValueSetValidatorService validator;

    public BundleAgendarTransformer(FhirServerConfig fhirServerConfig,
                                    MessageHeaderTransformer messageHeaderTransformer,
                                    PractitionerTransformer practitionerTransformer,
                                    PatientTransformer patientTransformer,
                                    OrganizationTransformer organizationTransformer,
                                    AppointmentTransformer appointmentTransformer,
                                    PractitionerRoleTransformer practitionerRoleTransformer,
                                    ValueSetValidatorService validator) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
        this.practitionerTransformer = practitionerTransformer;
        this.patientTransformer = patientTransformer;
        this.organizationTransformer = organizationTransformer;
        this.appointmentTransformer = appointmentTransformer;
        this.practitionerRoleTransformer = practitionerRoleTransformer;
        this.validator = validator;
    }

    public String buildBundle(String cmd) {
        ObjectMapper mapper = new ObjectMapper();
        OperationOutcome oo = new OperationOutcome();
        String res;

        Bundle b = new Bundle();
        b.getMeta().addProfile(bundleProfile);
        b.setType(Bundle.BundleType.MESSAGE);
        b.setTimestamp(new Date());

        JsonNode node;
        try {
            node = mapper.readTree(cmd);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(BundleAgendarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }

        // MessageHeader
        JsonNode datosSistema = node.get("datosSistema");
        MessageHeader messageHeader = null;
        ((ObjectNode)datosSistema).put("tipoEvento", "agendar");

        if(datosSistema != null) {
            messageHeader = messageHeaderTransformer.transform(datosSistema, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("datosSistema", oo);
        }

        // ServiceRequest
        JsonNode solicitudIC = node.get("solicitudIC");
        ServiceRequest sr = null;
        if(solicitudIC != null) {
            sr = buildServiceRequest(node, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("solicitudIC", oo);
        }

        // Patient
        JsonNode pacienteNode = node.get("paciente");
        ((ObjectNode)pacienteNode).put("tipoEvento", "agendar");

        Patient patient = null;
        if(pacienteNode != null) {
            patient = patientTransformer.transform(pacienteNode, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("paciente", oo);
        }

        // Practitioner (Profesional)
        JsonNode prestadorProfesional = node.get("prestadorProfesional");
        Practitioner practitionerProfesional = null;
        if(prestadorProfesional != null) {
            practitionerProfesional = practitionerTransformer.transform("profesional", prestadorProfesional, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("prestadorProfesional", oo);
        }

        // Practitioner (Administrativo)
        JsonNode prestadorAdmin = node.get("prestadorAdministrativo");
        Practitioner practitionerAdmin = null;
        if(prestadorAdmin != null) {
            practitionerAdmin = practitionerTransformer.transform("administrativo", prestadorAdmin, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("prestadorAdministrativo", oo);
        }

        // Organization (Agendador)
        /*
        JsonNode establecimientoAgendador = node.get("establecimiento");
        Organization organizationAgendador = null;
        if(establecimientoAgendador != null) {
            organizationAgendador = organizationTransformer.transform(establecimientoAgendador, oo, "agendador");
        } else {
            HapiFhirUtils.addNotFoundIssue("establecimientoAgendador", oo);
        }
        */

        // Organization (Resolutor)
        JsonNode establecimiento = node.get("establecimiento");
        Organization organization = null;
        if(establecimiento != null) {
            organization = organizationTransformer.transform(establecimiento, oo, "resolutor");
        } else {
            HapiFhirUtils.addNotFoundIssue("establecimientoResolutor", oo);
        }

        // PractitionerRole (Agendador)
        JsonNode rolProfesionalAgendador = node.get("rolDelProfesionalAgendador");
        PractitionerRole practitionerRoleAgendador = null;
        if(rolProfesionalAgendador != null) {
            practitionerRoleAgendador = practitionerRoleTransformer.transform(rolProfesionalAgendador, oo);
            Coding roleCode = new Coding("https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSPractitionerTipoRolLE", "agendador", "Agendador");
            if(rolProfesionalAgendador.has("glosa")){
                roleCode.setDisplay(HapiFhirUtils.readStringValueFromJsonNode("glosa",rolProfesionalAgendador));
            }
            CodeableConcept cc = new CodeableConcept(roleCode);
            practitionerRoleAgendador.addCode(cc);

            if(practitionerProfesional != null) {
                practitionerRoleAgendador.setPractitioner(new Reference("Practitioner/" + practitionerProfesional.getId()));
            }

            if(organization != null) {
                practitionerRoleAgendador.setOrganization(new Reference("Organization/" + organization.getIdentifierFirstRep().getValue()));
            }
        } else {
            HapiFhirUtils.addNotFoundIssue("rolDelProfesionalAgendador", oo);
        }

        // PractitionerRole (Resolutor)
        JsonNode rolProfesionalResolutor = node.get("rolDelProfesionalResolutor");
        PractitionerRole practitionerRoleResolutor = null;
        if(rolProfesionalResolutor != null) {
            practitionerRoleResolutor = practitionerRoleTransformer.transform(rolProfesionalResolutor, oo);
            Coding roleCode = new Coding("https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSPractitionerTipoRolLE", "resolutor", "Resolutor");
            CodeableConcept cc = new CodeableConcept(roleCode);
            practitionerRoleResolutor.addCode(cc);

            if(practitionerProfesional != null) {
                practitionerRoleResolutor.setPractitioner(new Reference("Practitioner/" + practitionerProfesional.getId()));
            }

            if(organization!= null) {
                practitionerRoleResolutor.setOrganization(new Reference("Organization/" + organization.getIdentifierFirstRep().getValue()));
            }
        } else {
            HapiFhirUtils.addNotFoundIssue("rolDelProfesionalResolutor", oo);
        }

        // Appointment
        JsonNode citaMedica = node.get("citaMedica");
        Appointment appointment = null;
        if(citaMedica != null) {
            appointment = appointmentTransformer.transform(citaMedica, oo);

            // Set patient reference
            String pacienteRef = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente", node);
            if(pacienteRef != null) {
                appointment.addParticipant(new Appointment.AppointmentParticipantComponent()
                        .setActor(new Reference(pacienteRef))
                        .setStatus(Appointment.ParticipationStatus.ACCEPTED));
            }

            // Set location reference
            if(citaMedica.has("ubicacion")) {
                appointment.addParticipant(new Appointment.AppointmentParticipantComponent()
                        .setActor(new Reference(citaMedica.get("ubicacion").asText()))
                        .setStatus(Appointment.ParticipationStatus.ACCEPTED));
            }

            // Set service request reference
            if(sr != null) {
                appointment.addBasedOn(new Reference("ServiceRequest/" + sr.getIdentifierFirstRep().getValue()));
            }
        } else {
            HapiFhirUtils.addNotFoundIssue("citaMedica", oo);
        }

        if (!oo.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(oo, fhirServerConfig.getFhirContext());
            return res;
        }

        // Add resources to bundle with their respective UUIDs
        IdType mHId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(mHId.getIdPart()).setResource(messageHeader);

        IdType sRId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(sRId.getIdPart()).setResource(sr);

        IdType patId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(patId.getIdPart()).setResource(patient);

        IdType pProfId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pProfId.getIdPart()).setResource(practitionerProfesional);

        IdType pAdminId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pAdminId.getIdPart()).setResource(practitionerAdmin);


        IdType orgResolutorId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(orgResolutorId.getIdPart()).setResource(organization);

        IdType pracRolAgendadorId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pracRolAgendadorId.getIdPart()).setResource(practitionerRoleAgendador);

        IdType pracRolResolutorId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pracRolResolutorId.getIdPart()).setResource(practitionerRoleResolutor);

        IdType appId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(appId.getIdPart()).setResource(appointment);

        // Set MessageHeader references
        setMessageHeaderReferences(messageHeader,
                new Reference(sRId.getValue()),
                new Reference(pracRolAgendadorId.getValue()),
                new Reference(appId.getValue()));

        res = HapiFhirUtils.resourceToString(b, fhirServerConfig.getFhirContext());
        return res;
    }

    public void setMessageHeaderReferences(MessageHeader m, Reference sr, Reference pr, Reference app) {
        m.setAuthor(pr);
        m.getFocus().add(sr);
        m.getFocus().add(app);
    }

    public ServiceRequest buildServiceRequest(JsonNode nodeOrigin, OperationOutcome oo) {
        String profile = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ServiceRequestLE";
        ServiceRequest sr = new ServiceRequest();
        sr.getMeta().addProfile(profile);

        sr.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

        JsonNode node = nodeOrigin.get("solicitudIC");

        try {
            Date d = HapiFhirUtils.readDateValueFromJsonNode("fechaSolicitudIC", node);
            sr.setAuthoredOn(d);
        } catch (ParseException ex) {
            Logger.getLogger(BundleAgendarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            HapiFhirUtils.addErrorIssue("fechaSolicitudIC", ex.getMessage(), oo);
        }

        String modalidadAtencion = HapiFhirUtils.readStringValueFromJsonNode("modalidadAtencion", node);
        if (modalidadAtencion != null) {
            Coding coding = VSModalidadAtencionEnum.fromCode(modalidadAtencion).getCoding();
            sr.getCategoryFirstRep().addCoding(coding);
        } else {
            HapiFhirUtils.addNotFoundIssue("modalidadAtencion", oo);
        }

        String idIC = HapiFhirUtils.readStringValueFromJsonNode("idInterconsulta", node);
        if (idIC != null) {
            Identifier identifierIC = new Identifier().setValue(idIC);
            sr.addIdentifier(identifierIC);
        } else {
            HapiFhirUtils.addNotFoundIssue("idInterconsulta", oo);
        }

        String pacienteRef = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente", nodeOrigin);
        if(pacienteRef != null) {
            sr.setSubject(new Reference(pacienteRef));
        } else {
            HapiFhirUtils.addNotFoundIssue("referenciaPaciente(para solicitudIC)", oo);
        }

        String codigoEstadoIC = HapiFhirUtils.readStringValueFromJsonNode("codigoEstadoIC", node);
        String glosaEstadoIC = HapiFhirUtils.readStringValueFromJsonNode("glosaEstadoIC", node);

        if(glosaEstadoIC == null) {
            HapiFhirUtils.addNotFoundIssue("glosaEstadoIC", oo);
        }

        if(codigoEstadoIC != null) {
            String csEIC = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEstadoInterconsulta";
            String vsEIC = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEstadoInterconsulta";
            String resValidacion = validator.validateCode(csEIC, codigoEstadoIC, "", vsEIC);
            if (resValidacion == null) {
                HapiFhirUtils.addErrorIssue(codigoEstadoIC, "solicitudIC.codigoEstadoIC No válido", oo);
            }
            CodeableConcept cc = new CodeableConcept();
            cc.addCoding(new Coding(
                    "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEstadoInterconsulta",
                    codigoEstadoIC,
                    glosaEstadoIC));

            Extension extensionEstadoIC = new Extension();
            extensionEstadoIC.setUrl("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEstadoInterconsultaCodigoLE");
            extensionEstadoIC.setValue(cc);
            sr.addExtension(extensionEstadoIC);
        } else {
            HapiFhirUtils.addNotFoundIssue("codigoEstadoIC", oo);
        }

        return sr;
    }
}