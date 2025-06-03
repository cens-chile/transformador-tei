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
    ValueSetValidatorService validator;

    public BundleAtenderTransformer(FhirServerConfig fhirServerConfig,
                                    MessageHeaderTransformer messageHeaderTransformer,
                                    PractitionerTransformer practitionerTransformer,
                                    PatientTransformer patientTransformer,
                                    OrganizationTransformer organizationTransformer,
                                    CarePlanTransformer carePlanTransformer,
                                    ValueSetValidatorService validator) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
        this.practitionerTransformer = practitionerTransformer;
        this.patientTransformer = patientTransformer;
        this.organizationTransformer = organizationTransformer;
        this.carePlanTransformer = carePlanTransformer;
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
            sr = buildServiceRequest(get, oo);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC", oo);


        // Prestador
        get = node.get("prestador");
        String tipoPrestador = HapiFhirUtils.readStringValueFromJsonNode("tipoPrestador", get);
        if(!tipoPrestador.toLowerCase().equals("profesional") && !tipoPrestador.toLowerCase().equals("administrativo")){
            HapiFhirUtils.addErrorIssue("Tipo Prestador", "Dato no válido", oo);
        }
        Practitioner practitioner = null;

        if(get!=null && tipoPrestador != null){
            practitioner = practitionerTransformer.transform(tipoPrestador,get, oo);
        }
        else{
            HapiFhirUtils.addNotFoundIssue("Prestador", oo);
        }

        // Rol del Profesional (practitionerRol)
        get = node.get("rolDelProfesional");
        PractitionerRole practitionerRole = null;
        if(get != null){
            practitionerRole = practitionerRoleTransformer.transform(get, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("Rol de profesional no definido", oo);
        }

        get = node.get("establecimiento");
        Organization organization = null;
        if(get != null){
            organization = organizationTransformer.transform(get, oo,"");
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la organización(establecimiento)", oo);
        }

        //Encuentro
        get = node.get("encuentro");
        Encounter encounter = null;
        if(get != null){
            encounter = EncounterTransformer.transform(get, oo,"Atender");
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la organización(encuentro)", oo);
        }


        get = node.get("planDeAtencion");
        CarePlan  careplan = null;
        if(get != null){
            careplan = carePlanTransformer.transform(get, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos del Plan de Atención", oo);
        }


        /***********Condición
         get = node.get("condicion");
         Condition condition = null;
         if(get != null){
         condition = ConditionTransformer.transform(get, oo);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la Condición del paciente", oo);
         }

         */


        /***********
         get = node.get("alergiaIntolerancia");
         AllergyIntolerance allergyIntolerance = null;
         if(get != null){
         allergyIntolerance = AllergyIntoleranceTransformer.transform(get, oo);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la AlergiaIntolerancia del paciente", oo);
         }

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
         get = node.get("");
         Observation observation = null;
         if(get != null){
         observation = ObservationTransformer.transform(get, oo);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la Resultados de Exámenes del paciente", oo);
         }

         */

        /*********** Solicitud Exámen
         get = node.get("");
         Observation observation = null;
         if(get != null){
         observation = ObservationTransformer.transform(get, oo);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la Resultados de Exámenes del paciente", oo);
         }

         */


        /*********** Anamnesis
         get = node.get("");
         Anamnesis anamnesis = null;
         if(get != null){
         anamnesis = AnamnesisTransformer.transform(get, oo);
         } else {
         HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la Resultados de la Anamnesis del paciente", oo);
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
