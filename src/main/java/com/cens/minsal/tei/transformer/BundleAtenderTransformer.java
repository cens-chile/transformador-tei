/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSModalidadAtencionEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.Json;
import org.hl7.fhir.exceptions.FHIRException;
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
public class BundleAtenderTransformer {

    FhirServerConfig fhirServerConfig;
    static final String bundleProfile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/BundleAtenderLE";

    MessageHeaderTransformer messageHeaderTransformer;
    PractitionerTransformer practitionerTransformer;
    PractitionerRoleTransformer practitionerRoleTransformer;
    PatientTransformer patientTransformer;
    OrganizationTransformer organizationTransformer;
    CarePlanTransformer carePlanTransformer;
    ValueSetValidatorService validator;

    public BundleAtenderTransformer(FhirServerConfig fhirServerConfig,
                                    MessageHeaderTransformer messageHeaderTransformer,
                                    PractitionerTransformer practitionerTransformer,
                                    PatientTransformer patientTransformer,
                                    OrganizationTransformer organizationTransformer,
                                    CarePlanTransformer carePlanTransformer,
                                    PractitionerRoleTransformer practitionerRoleTransformer,
                                    ValueSetValidatorService validator) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
        this.practitionerTransformer = practitionerTransformer;
        this.patientTransformer = patientTransformer;
        this.organizationTransformer = organizationTransformer;
        this.carePlanTransformer = carePlanTransformer;
        this.validator = validator;
        this.practitionerRoleTransformer = practitionerRoleTransformer;
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
            Logger.getLogger(BundleAtenderTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }



        
        JsonNode get = node.get("datosSistema");
        MessageHeader messageHeader = null;
        ((ObjectNode)get).put("tipoEvento", "atender");

        if(get!=null)
            messageHeader = 
                messageHeaderTransformer.transform(get, oo);
        else
            HapiFhirUtils.addNotFoundIssue("datosSistema", oo);


        get = node.get("solicitudIC");
        ServiceRequest sr = null;
        if(get!=null)
            sr = buildServiceRequest(node, oo);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC", oo);


        // Prestador
        get = node.get("prestador");
        Practitioner practitioner = null;

        try {
            String tipoPrestador = HapiFhirUtils.readStringValueFromJsonNode("tipoPrestador", get);
            if (tipoPrestador == null) HapiFhirUtils.addNotFoundIssue("tipoPrestador", oo);
            if (!tipoPrestador.toLowerCase().equals("profesional") && !tipoPrestador.toLowerCase().equals("administrativo")) {
                HapiFhirUtils.addErrorIssue("Tipo Prestador", "Dato no válido", oo);
            }

            practitioner = practitionerTransformer.transform(tipoPrestador, get, oo);

        }catch (Exception e){
            HapiFhirUtils.addNotFoundIssue("prestador.tipoPrestador",oo);
        }

        get = node.get("establecimiento");
        Organization organization = null;
        if(get != null){
            organization = organizationTransformer.transform(get, oo,"");
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la organización(establecimiento)", oo);
        }

        // Rol del Profesional (practitionerRol)
        get = node.get("rolDelProfesional");
        PractitionerRole practitionerRole = null;
        if(get != null){
            practitionerRole = practitionerRoleTransformer.transform(get, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("Rol de profesional no definido", oo);
        }
        Coding roleCode = new Coding("https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSPractitionerTipoRolLE", "atendedor", "Atendedor");
        CodeableConcept cc = new CodeableConcept(roleCode);
        practitionerRole.addCode(cc);

        try {
            practitionerRole.setPractitioner(new Reference("Practitioner/" + practitioner.getId()));
        }catch (Exception e){
            HapiFhirUtils.addNotFoundIssue("Practitioner", oo);
        }
        practitionerRole.setOrganization(new Reference("Organization/"+organization.getIdentifier().get(0).getValue().toString()));



        //Encuentro
        get = node.get("encuentro");
        Encounter encounter = null;
        if(get != null){
            EncounterTransformer encounterTransformer = new EncounterTransformer(validator);
            encounter = encounterTransformer.transform(get, oo,"Atender");
            try {
                String refPaciente = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente", node);
                encounter.setSubject(new Reference(refPaciente));
            }catch (Exception e){
                HapiFhirUtils.addNotFoundIssue("referenciaPaciente(para encuentro)", oo);
            }
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la organización(encuentro)", oo);
        }


        get = node.get("planDeAtencion");
        CarePlan  careplan = null;

        if(get != null){
            careplan = carePlanTransformer.transform(get, oo);
            String paciente = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente", node);
            if(paciente != null){
                careplan.setSubject(new Reference(paciente));
            }else{
                HapiFhirUtils.addNotFoundIssue("planDeAtencion.referenciaPaciente",oo);
            }
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos del Plan de Atención(planDeAtencion)", oo);
        }

        String descripcionPlan = HapiFhirUtils.readStringValueFromJsonNode("descripcion",get);
        if(descripcionPlan == null){
            HapiFhirUtils.addNotFoundIssue("No se encontró Descripción del Plan de Atención", oo);
        } else{
            careplan.setDescription(descripcionPlan);
        }

        //Construir Diagnostico
        ConditionTransformer conditionTransformer = new ConditionTransformer(validator);
        Condition cond = null;
        if(node.get("diagnostico")!=null){
            cond = conditionTransformer.transform(node.get("diagnostico"), oo,"diagnostico");
        }
        else
            HapiFhirUtils.addNotFoundIssue("diagnostico", oo);





         get = node.get("paciente");
         Patient patient = null;
         if(get != null){
            PatientTransformer pt = new PatientTransformer(validator);
            ((ObjectNode)get).put("tipoEvento", "atender");



             patient = pt.transform(get, oo);
         } else HapiFhirUtils.addNotFoundIssue("paciente", oo);


         /***********
         */

        /*********** Observación Resultados Exámen
         get = node.get("");
         Observation observation = null;
         if(get != null){
         observation = ObservationTransformer.transform(get, oo);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la Resultados de Exámenes del paciente", oo);
         }
         */

        /*********** Solicitud de Medicamentos
         get = node.get("solicitudDeMedicamentos");
         MedicationRequest medReq = null;
         if(get != null){
         medReq = MedicationRequestTransformer.transform(get, oo);
         } else {
         HapiFhirUtils.addNotFoundIssue("solicitudDeMedicamentos no encontrado", oo);
         }

         */


        if (!oo.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(oo,fhirServerConfig.getFhirContext());
            return res;
        }


        //Agrega recursos con sus respectivos UUID al bundle de respuesta
        IdType mHId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(mHId.getIdPart())
                .setResource(messageHeader);

        IdType sRId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(sRId.getIdPart())
                .setResource(sr);

        IdType patId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(patId.getIdPart())
                .setResource(patient);


        IdType pAId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pAId.getIdPart())
                .setResource(practitioner);

        IdType pracRolId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pracRolId.getIdPart())
                .setResource(practitionerRole);


        IdType orgId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(orgId.getIdPart())
                .setResource(organization);

        IdType cpId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(cpId.getIdPart())
                .setResource(careplan);

        IdType condId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(condId.getIdPart())
                .setResource(cond);

        IdType encId = IdType.newRandomUuid();
        b.addEntry().setFullUrl((encId.getIdPart())).setResource(encounter);

        setMessageHeaderReferences(messageHeader, new Reference(sRId.getValue()), new Reference(pracRolId.getValue()), new Reference(encId.getValue()));
        
        
        res = HapiFhirUtils.resourceToString(b, fhirServerConfig.getFhirContext());
        return res;
    }
    
    
    public void setMessageHeaderReferences(MessageHeader m, Reference sr, Reference pr, Reference encR){
        m.setAuthor(pr);
        m.getFocus().add(sr);
        m.getFocus().add(encR);
    }
    
    
    public ServiceRequest buildServiceRequest(JsonNode nodeOrigin, OperationOutcome oo){
        String profile ="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ServiceRequestLE";
        ServiceRequest sr = new ServiceRequest();
        sr.getMeta().addProfile(profile);
        
        sr.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

        JsonNode node = nodeOrigin.get("solicitudIC");

        try {
            Date d = HapiFhirUtils.readDateValueFromJsonNode("fechaSolicitudIC", node);
            sr.setAuthoredOn(d);
        } catch (ParseException ex) {
            Logger.getLogger(BundleAtenderTransformer.class.getName()).log(Level.SEVERE, null, ex);
            HapiFhirUtils.addErrorIssue("fechaSolicitudIC", ex.getMessage(), oo);
        }
        String modalidadAtencion = HapiFhirUtils.readStringValueFromJsonNode("modalidadAtencion", node);
        Coding coding = VSModalidadAtencionEnum.fromCode(modalidadAtencion).getCoding();
        sr.getCategoryFirstRep().addCoding(coding);

        String idIC = HapiFhirUtils.readStringValueFromJsonNode("idInterconsulta", node);
        if (idIC == null) HapiFhirUtils.addNotFoundIssue("idInterconsulta", oo);
        Identifier identifierIC = new Identifier().setValue(idIC);
        sr.addIdentifier(identifierIC);

        Reference pacienteRef = new Reference(HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente",nodeOrigin));
        if(pacienteRef != null ){
            sr.setSubject(pacienteRef);
        }else HapiFhirUtils.addNotFoundIssue("referenciaPaciente(para solicitudIC)",oo);

        JsonNode practitionerJ = nodeOrigin.get("prestador");

        if(practitionerJ == null){
            HapiFhirUtils.addNotFoundIssue("prestador", oo);
        }
        try {
            String idPractitioner = HapiFhirUtils.readStringValueFromJsonNode("id", practitionerJ);
            if (practitionerJ != null && idPractitioner != null) {
                List<Reference> practL = new ArrayList<>();
                Reference practitionerRef = new Reference("Practitioner/" + idPractitioner);
                practL.add(practitionerRef);
                sr.setPerformer(practL);
            } else HapiFhirUtils.addNotFoundIssue("Prestador.id",oo);

        } catch (Exception e) {
            HapiFhirUtils.addNotFoundIssue("practitioner.id", oo);
        }



        String codigoEstadoIC = HapiFhirUtils.readStringValueFromJsonNode("codigoEstadoIC", node);
        String glosaEstadoIC = HapiFhirUtils.readStringValueFromJsonNode("glosaEstadoIC", node);

        if(glosaEstadoIC == null) HapiFhirUtils.addNotFoundIssue("glosaEstadoIC", oo);

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
        } else HapiFhirUtils.addNotFoundIssue("codigoEstadoIC", oo);

        return sr;
    }
}
