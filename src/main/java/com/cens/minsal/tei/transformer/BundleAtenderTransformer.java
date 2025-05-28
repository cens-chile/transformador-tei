/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSModalidadAtencionEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;
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


    public BundleAtenderTransformer(FhirServerConfig fhirServerConfig,
                                    MessageHeaderTransformer messageHeaderTransformer,
                                    PractitionerTransformer practitionerTransformer,
                                    PatientTransformer patientTransformer,
                                    OrganizationTransformer organizationTransformer,CarePlanTransformer carePlanTransformer) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
        this.practitionerTransformer = practitionerTransformer;
        this.patientTransformer = patientTransformer;
        this.organizationTransformer = organizationTransformer;
        this.carePlanTransformer = carePlanTransformer;
    }
    
    
    public String buildBundle(String cmd) {
        ObjectMapper mapper = new ObjectMapper();
        
        String res;
        OperationOutcome out = new OperationOutcome();
        
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
        if(get!=null)
            messageHeader = 
                messageHeaderTransformer.coreDataSetTEIToMessageHeader(get, out);
        else
            HapiFhirUtils.addNotFoundIssue("datosSistema", out);


        get = node.get("solicitudIC");
        ServiceRequest sr = null;
        if(get!=null)
            sr = buildServiceRequest(get, out);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC", out);


        // Prestador
        get = node.get("prestadorProfesional");
        Practitioner practitioner = null;
        if(get!=null){
            practitioner = practitionerTransformer.transform("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PractitionerProfesionalLE",get, out);
        }
        else {
            HapiFhirUtils.addNotFoundIssue("Prestador", out);
        }

        // Rol del Profesional (practitionerRol)
        get = node.get("rolDelProfesional");
        PractitionerRole practitionerRole = null;
        if(get != null){
            practitionerRole = practitionerRoleTransformer.transform(get, out);
        } else {
            HapiFhirUtils.addNotFoundIssue("Rol de profesional no definido", out);
        }
        get = node.get("establecimiento");
        Organization organization = null;
        if(get != null){
            organization = organizationTransformer.transform(get, out);
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la organización", out);
        }

        /***********Encuentro
        get = node.get("encuentro");
        Encounter encounter = null;
        if(get != null){
            encounter = EncounterTransformer.transform(get, out);
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la organización", out);
        }

         */

        get = node.get("planDeAtencion");
        CarePlan  careplan = null;
        if(get != null){
            careplan = carePlanTransformer.transform(get, out);
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos del Plan de Atención", out);
        }


        /***********Condición
         get = node.get("condicion");
         Condition condition = null;
         if(get != null){
         condition = ConditionTransformer.transform(get, out);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la Condición del paciente", out);
         }

         */


        /***********
         get = node.get("alergiaIntolerancia");
         AllergyIntolerance allergyIntolerance = null;
         if(get != null){
         allergyIntolerance = AllergyIntoleranceTransformer.transform(get, out);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la AlergiaIntolerancia del paciente", out);
         }

         */

        /*********** Observación Resultados Exámen
         get = node.get("");
         Observation observation = null;
         if(get != null){
         observation = ObservationTransformer.transform(get, out);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la Resultados de Exámenes del paciente", out);
         }

         */

        /*********** Solicitud de Medicamentos
         get = node.get("");
         Observation observation = null;
         if(get != null){
         observation = ObservationTransformer.transform(get, out);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la Resultados de Exámenes del paciente", out);
         }

         */

        /*********** Solicitud Exámen
         get = node.get("");
         Observation observation = null;
         if(get != null){
         observation = ObservationTransformer.transform(get, out);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la Resultados de Exámenes del paciente", out);
         }

         */


        /*********** Anamnesis
         get = node.get("");
         Anamnesis anamnesis = null;
         if(get != null){
         anamnesis = AnamnesisTransformer.transform(get, out);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la Resultados de la Anamnesis del paciente", out);
         }

         */


        if (!out.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(out,fhirServerConfig.getFhirContext());
            return res;
        }


        //Agrega recursos con sus respectivos UUID al bundle de respuesta
        IdType mHId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(mHId.getIdPart())
                .setResource(messageHeader);

        IdType sRId = IdType.newRandomUuid();

        b.addEntry().setFullUrl(sRId.getIdPart())
                .setResource(sr);

        IdType pAId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pAId.getIdPart())
                .setResource(practitioner);

        IdType pracRolId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pracRolId.getIdPart())
                .setResource(practitionerRole);


        IdType orgId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(orgId.getIdPart())
                .setResource(organization);

        setMessageHeaderReferences(messageHeader, null, null);
        
        
        res = HapiFhirUtils.resourceToString(b, fhirServerConfig.getFhirContext());
        return res;
    }
    
    
    public void setMessageHeaderReferences(MessageHeader m, Reference sr, Reference pr){
        m.setAuthor(pr);
        m.getFocus().add(sr);
    }
    
    
    public ServiceRequest buildServiceRequest(JsonNode node, OperationOutcome oo){
        String profile ="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ServiceRequestLE";
        ServiceRequest sr = new ServiceRequest();
        sr.getMeta().addProfile(profile);
        
        sr.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        
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
        return sr;
    }
}
