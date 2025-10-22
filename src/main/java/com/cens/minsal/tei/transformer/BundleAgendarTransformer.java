package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.utils.JsonUniqueKeyValidator;
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
import java.util.UUID;
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
            JsonUniqueKeyValidator.validateUniqueKeys(cmd, oo);
            node = mapper.readTree(cmd);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(BundleAgendarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }

        JsonNode datosSistema = node.get("datosSistema");
        boolean datosValid = HapiFhirUtils.validateObjectInJsonNode("datosSistema", datosSistema,oo,true);
        MessageHeader messageHeader = null;
        if(datosValid){
            ((ObjectNode)datosSistema).put("tipoEvento", "agendar");
            messageHeader = messageHeaderTransformer.transform(datosSistema, oo);
        }

        // ServiceRequest
        JsonNode solicitudIC = node.get("solicitudIC");
        ServiceRequest sr = null;
        boolean srValid = HapiFhirUtils.validateObjectInJsonNode("solicitudIC", solicitudIC,oo, true);
        if(srValid) {
            sr = buildServiceRequest(node, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("solicitudIC", oo);
        }

        JsonNode prestadorProfesionalNode = node.get("prestadorProfesional");
        Practitioner practitionerProfesional = null;
        boolean practProfValid = HapiFhirUtils.validateObjectInJsonNode("prestadorProfesional", prestadorProfesionalNode,oo, true);
        if(practProfValid) {
            practitionerProfesional = practitionerTransformer.transform("profesional", prestadorProfesionalNode, oo);
        }

        JsonNode prestadorAdminNode = node.get("prestadorAdministrativo");
        boolean presAdminNodeVal = HapiFhirUtils.validateObjectInJsonNode("prestadorAdministrativo", prestadorAdminNode, oo, true);
        Practitioner practitionerAdmin = null;
        if(presAdminNodeVal) {
            practitionerAdmin = practitionerTransformer.transform("administrativo", prestadorAdminNode, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("prestadorAdministrativo", oo);
        }



        // Organization (Resolutor)
        JsonNode establecimiento = node.get("establecimiento");
        Organization organization = null;
        boolean validate = HapiFhirUtils.validateObjectInJsonNode("establecimiento", establecimiento, oo,true);
        if(validate){
            //Construir Organización de destino
            organization = organizationTransformer.transform(establecimiento, oo,"establecimiento");
        }

        // PractitionerRole (Agendador)
        PractitionerRole practitionerRoleAgendador = null;
        if(organization != null && practitionerAdmin != null) {
            practitionerRoleAgendador = practitionerRoleTransformer.buildPractitionerRole("agendador", organization, practitionerAdmin);

        } else
            HapiFhirUtils.addErrorIssue("rol del agendador","error en establecimiento o prestador administrativo", oo);


        // PractitionerRole (Resolutor)
    PractitionerRole practitionerRoleAtendedor = null;
    if (organization != null && practitionerProfesional != null){
           practitionerRoleAtendedor = practitionerRoleTransformer.buildPractitionerRole("atendedor", organization, practitionerProfesional);
        } else {
            HapiFhirUtils.addNotFoundIssue("rolDelProfesionalResolutor", oo);
        }

        // Appointment
        JsonNode citaMedica = node.get("cita");
    boolean citaValid = HapiFhirUtils.validateObjectInJsonNode("cita", citaMedica,oo,true);

            Appointment appointment = null;
        if (citaValid){
                appointment = appointmentTransformer.transform(node, oo);
                if (practitionerRoleAtendedor != null){
                    String vs ="http://hl7.org/fhir/ValueSet/resource-types";
                    String cs = "http://hl7.org/fhir/resource-types";

                    Coding coding = new Coding(cs, "PractitionerRole", "PractitionerRole");
                    CodeableConcept cc = new CodeableConcept(coding);

                    appointment.addParticipant(new Appointment.AppointmentParticipantComponent()
                            .setActor(new Reference(practitionerRoleAtendedor).setType(coding.getCode()))
                            .setStatus(Appointment.ParticipationStatus.ACCEPTED));
                }

                // Set service request reference
                if(sr != null) {
                    appointment.addBasedOn(new Reference(sr));
                }
            } else {
                HapiFhirUtils.addNotFoundIssue("citaMedica", oo);
            }

            if (!oo.getIssue().isEmpty()) {
                res = HapiFhirUtils.resourceToString(oo, fhirServerConfig.getFhirContext());
                return res;
            }

            // Add resources to bundle with their respective UUIDs
            HapiFhirUtils.addResourceToBundle(b,messageHeader);
            HapiFhirUtils.addResourceToBundle(b,sr);
            HapiFhirUtils.addResourceToBundle(b,practitionerProfesional);
            HapiFhirUtils.addResourceToBundle(b,practitionerAdmin);
            HapiFhirUtils.addResourceToBundle(b,organization);
            HapiFhirUtils.addResourceToBundle(b,practitionerRoleAgendador);
            HapiFhirUtils.addResourceToBundle(b,practitionerRoleAtendedor);
            if(appointment.getId()== null) {
                HapiFhirUtils.addResourceToBundle(b, appointment);
            }else{
                b.addEntry().setResource(appointment);
            }


            // Set MessageHeader references
            setMessageHeaderReferences(messageHeader,
                    new Reference(sr),
                    new Reference(practitionerRoleAgendador),
                    new Reference(appointment));

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

        sr.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
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

            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSModalidadAtencionCodigo";
            String cs  = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSModalidadAtencionCodigo";
            String validate = validator.validateCode(cs, modalidadAtencion, "",vs);
            if(validate != null){
                Coding coding = VSModalidadAtencionEnum.fromCode(modalidadAtencion).getCoding();
                sr.getCategoryFirstRep().addCoding(coding);
            } else
                HapiFhirUtils.addNotFoundCodeIssue("solicitudIC.modalidadAtencion",oo);

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

        String codigoEstadoIC = "5";

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
                    resValidacion));

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