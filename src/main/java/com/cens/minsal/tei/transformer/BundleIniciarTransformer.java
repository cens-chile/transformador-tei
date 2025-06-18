/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSDerivadoParaEnum;
import com.cens.minsal.tei.valuesets.VSModalidadAtencionEnum;
import com.cens.minsal.tei.valuesets.VSEstadoInterconsultaEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class BundleIniciarTransformer {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BundleIniciarTransformer.class);
    FhirServerConfig fhirServerConfig;
    static final String bundleProfile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/BundleIniciarLE";
    static final String snomedSystem = "http://snomed.info/sct";
    PatientTransformer patientTr;
    MessageHeaderTransformer messageHeaderTransformer;
    EncounterTransformer encTransformer;
    OrganizationTransformer orgTransformer;
    AllergyIntoleranceTransformer allInTransformer;
    QuestionnaireResponseTransformer questTransformer;
    ServiceRequestTransformer serTransformer;
    PractitionerTransformer praTransformer;
    PractitionerRoleTransformer praRoleTransformer;
    ValueSetValidatorService validator;
    
    public BundleIniciarTransformer(FhirServerConfig fhirServerConfig,
            MessageHeaderTransformer messageHeaderTransformer,
            EncounterTransformer encTransformer,
            PatientTransformer patientTr,
            OrganizationTransformer orgTransformer,
            ValueSetValidatorService validator,
            QuestionnaireResponseTransformer questTransformer,
            AllergyIntoleranceTransformer allInTransformer,
            ServiceRequestTransformer serTransformer,
            PractitionerTransformer praTransformer,
            PractitionerRoleTransformer praRoleTransformer) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
        this.encTransformer = encTransformer;
        this.patientTr = patientTr;
        this.orgTransformer = orgTransformer;
        this.allInTransformer = allInTransformer;
        this.questTransformer = questTransformer;
        this.serTransformer = serTransformer;
        this.praTransformer = praTransformer;
        this.praRoleTransformer = praRoleTransformer;
        this.validator = validator;
    }
    
    
    public String buildBundle(String cmd){
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
            java.util.logging.Logger.getLogger(BundleIniciarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }
        
        JsonNode get = node.get("datosSistema");
        MessageHeader messageHeader = null;
        ((ObjectNode)get).put("tipoEvento", "iniciar");


        if(get!=null)
            messageHeader = 
                messageHeaderTransformer.transform(node.get("datosSistema"), out);
        else
            HapiFhirUtils.addNotFoundIssue("datosSistema", out);

        JsonNode paciente = node.get("paciente");
        Patient patient = null;
        if(paciente!=null)
            patient = patientTr.transform(paciente, out);
        else
            HapiFhirUtils.addNotFoundIssue("paciente", out);
            
        get = node.get("profesionalClinicoSolicita");
        Practitioner practitioner = null;
        if(get!=null){
            practitioner = praTransformer.transform("profesional", get, out);
        }else
            HapiFhirUtils.addNotFoundIssue("profesionalClinicoSolicita", out);
        
        get = node.get("solicitudIC");
        ServiceRequest sr = null;
        if(get!=null)
            sr = buildServiceRequest(get, out);  
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC", out);

        
        
        //Construir Encuentro
        Encounter enc = encTransformer.transform(node, out, "iniciar");
        
        
        //Construir Organización que inicia la IC
        Organization org = null;
        try{
            get = node.get("establecimiento").get("origen");
            if(get!=null)
                org = orgTransformer.transform(get, out,"establecimiento.origen");   
        }catch(NullPointerException ex){
            HapiFhirUtils.addNotFoundIssue("establecimiento.origen", out);
        }
        //Contruir Indice Comorbilidad
        Observation indiceComorbilidad  = null;
        if(node.get("indiceComorbilidad")!=null){
           indiceComorbilidad = ObservationTransformer.buildIndiceComporbilidad(node.get("indiceComorbilidad"),out); 
        }
        
        //Construir Diagnostico
        ConditionTransformer conditionTransformer = new ConditionTransformer(validator);
        Condition cond = null;
        if(node.get("diagnostico")!=null){
            cond = conditionTransformer.transform(node.get("diagnostico"), out,"diagnostico");
        }
        else
            HapiFhirUtils.addNotFoundIssue("diagnostico", out);
        
        //agregar discapacidad
        Boolean presentaDiscapacidad = HapiFhirUtils.readBooleanValueFromJsonNode("presentaDiscapacidad", node);
        Observation discapacidad = new Observation();
        if(node.has("presentaDiscapacidad")) {
            if (!node.get("presentaDiscapacidad").isBoolean())
                HapiFhirUtils.addInvalidIssue("resolutividadAPS", out);
             discapacidad = ObservationTransformer.buildDiscapacidad(presentaDiscapacidad);
        }
        
        Boolean cuidador = HapiFhirUtils.readBooleanValueFromJsonNode("cuidador", node);
        Observation cuidadorObservation = null;
        if(node.has("cuidador")) {
            if (!node.get("cuidador").isBoolean())
                HapiFhirUtils.addInvalidIssue("cuidador", out);
            cuidadorObservation = ObservationTransformer.buildCuidador(cuidador);
        }
        //Se agregan resultados exámenes realizados
        List<Observation> examenes = new ArrayList();
        JsonNode resultados = node.get("resultadoExamenes");
        if(resultados!=null){
            ObservationTransformer ot = new ObservationTransformer(validator);
            examenes = ot.buildResultadoExamen(resultados, out);
        }
        
        
        //Se agregan alergias
        List<AllergyIntolerance> alergias = new ArrayList();
        JsonNode allNode = node.get("alergias");
        if(allNode!=null){
            alergias = allInTransformer.transform(allNode, out);
        }
        
        //Se agrega motivo de derivacion
        QuestionnaireResponse motivoDerivacion = 
                questTransformer.transform(node, out);
        
        
        
        //Se agrega exámen solicitado
        List<ServiceRequest> examenSolicitados= serTransformer.buildSolicitudExamen(node, out);
       
        
        if (!out.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(out,fhirServerConfig.getFhirContext());
            return res;
        }
        
        PractitionerRole praRole = praRoleTransformer.buildPractitionerRole("iniciador", org, practitioner);
        
        
        IdType mHId = IdType.newRandomUuid();
        System.out.println("mHId = " + mHId.getIdPart());
        b.addEntry().setFullUrl(mHId.getIdPart())
                .setResource(messageHeader);
        setMessageHeaderReferences(messageHeader, new Reference(sr), new Reference(praRole));
        
        IdType patId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(patId.getIdPart())
                .setResource(patient);
        
        IdType pracId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pracId.getIdPart())
                .setResource(practitioner);
        
        
        addResourceToBundle(b,praRole);
        
       
        IdType sRId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(sRId.getIdPart())
                .setResource(sr);
        setServiceRequestReferences(sr,patient,enc, praRole,cond,alergias,
                indiceComorbilidad,cuidadorObservation,discapacidad,
                motivoDerivacion,examenSolicitados);
        
        IdType encId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(encId.getIdPart())
                .setResource(enc);
        
        
        IdType iCId = IdType.newRandomUuid();
            b.addEntry().setFullUrl(iCId.getIdPart())
                .setResource(indiceComorbilidad);
            
        IdType orgId = IdType.newRandomUuid();
            b.addEntry().setFullUrl(orgId.getIdPart())
                .setResource(org);
            
            
        IdType condId = IdType.newRandomUuid();
            b.addEntry().setFullUrl(condId.getIdPart())
                .setResource(cond);
        
        IdType disId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(disId.getIdPart())
                .setResource(discapacidad);   
        
        if(cuidadorObservation!=null){
            IdType cuidadorId = IdType.newRandomUuid();
            b.addEntry().setFullUrl(cuidadorId.getIdPart())
                    .setResource(cuidadorObservation);   
            cuidadorObservation.setSubject(new Reference(patient));

        }
        
        
        for(Observation ob : examenes){
            IdType obId = IdType.newRandomUuid();
            b.addEntry().setFullUrl(obId.getIdPart())
                .setResource(ob);  
            ob.setSubject(new Reference(patient));
        }
        
        for(AllergyIntolerance aler : alergias){
            IdType alerId = IdType.newRandomUuid();
            b.addEntry().setFullUrl(alerId.getIdPart())
                .setResource(aler);
            aler.setPatient(new Reference(patient));
        }
        
        
        IdType motId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(motId.getIdPart())
                .setResource(motivoDerivacion); 
        motivoDerivacion.setAuthor(new Reference(praRole));
        motivoDerivacion.setSubject(new Reference(patient));
        
        
        
        for(ServiceRequest s : examenSolicitados){
            IdType sId = IdType.newRandomUuid();
            b.addEntry().setFullUrl(sId.getIdPart())
                    .setResource(s); 
            s.setSubject(new Reference(patient));
            s.getBasedOn().add(new Reference(sr));
        }
        
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
            //System.out.println("d = " + d.toString());
        } catch (ParseException ex) {
            Logger.getLogger(BundleIniciarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            HapiFhirUtils.addErrorIssue("fechaSolicitudIC", ex.getMessage(), oo);
        }
        String modalidadAtencion = HapiFhirUtils.readIntValueFromJsonNode("modalidadAtencion", node);
        if(modalidadAtencion!=null){
            VSModalidadAtencionEnum fromCode = VSModalidadAtencionEnum.fromCode(modalidadAtencion);
            if(fromCode!=null){
                Coding coding = VSModalidadAtencionEnum.fromCode(modalidadAtencion).getCoding();
                sr.getCategoryFirstRep().addCoding(coding);
            } 
            else
                HapiFhirUtils.addErrorIssue("modalidadAtencion","código no encontrado", oo);
        }
        else
             HapiFhirUtils.addNotFoundIssue("modalidadAtencion", oo);
        
        String derivadoPara = HapiFhirUtils.readIntValueFromJsonNode("derivadoPara", node);
        if(derivadoPara!=null){
            VSDerivadoParaEnum fromCode = VSDerivadoParaEnum.fromCode(derivadoPara);
            if(fromCode!=null){
                Coding coding = VSDerivadoParaEnum.fromCode(derivadoPara).getCoding();
                sr.getReasonCodeFirstRep().addCoding(coding);
            } 
            else
                HapiFhirUtils.addErrorIssue("derivadoPara","código no encontrado", oo);
        }
        else
             HapiFhirUtils.addNotFoundIssue("derivadoPara", oo);
        
        
        
        Coding c = new Coding(snomedSystem,"103696004","Patient referral to specialist");
        sr.getCode().addCoding(c);
        
        String prioridadIc = HapiFhirUtils.readStringValueFromJsonNode("prioridadIc", node);
        if(prioridadIc!=null){
            
            if(prioridadIc.equals("routine")){
                sr.setPriority(ServiceRequest.ServiceRequestPriority.ROUTINE);
            } 
            else if(prioridadIc.equals("urgent")){
                sr.setPriority(ServiceRequest.ServiceRequestPriority.URGENT);
            } 
            else
                HapiFhirUtils.addErrorIssue("prioridadIc","código no encontrado", oo);
        }
        else
             HapiFhirUtils.addNotFoundIssue("prioridadIc", oo);
        
        
        
        
        Boolean atPreferente = HapiFhirUtils.readBooleanValueFromJsonNode("atencionPreferente", node);
        if(atPreferente!=null){
            if(!node.get("atencionPreferente").isBoolean())
                HapiFhirUtils.addInvalidIssue("atencionPreferente", oo);
            
            Extension extAtPreferente = 
                HapiFhirUtils.buildBooleanExt(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionBoolAtencionPreferente",
                 atPreferente);
            sr.addExtension(extAtPreferente);
            
        }
        
        Boolean resolutividadAPS = HapiFhirUtils.readBooleanValueFromJsonNode("resolutividadAPS", node);
        if(resolutividadAPS!=null){
            if(!node.get("resolutividadAPS").isBoolean())
                HapiFhirUtils.addInvalidIssue("resolutividadAPS", oo);
            
            Extension extResolutividadAPS = 
                HapiFhirUtils.buildBooleanExt(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionBoolResolutividadAPS",
                 resolutividadAPS);
            sr.addExtension(extResolutividadAPS);
            
        }
        
        c = new Coding("https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSorigenInterconsulta"
                ,"1","APS");
        Extension origenIC = HapiFhirUtils.buildExtension(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionOrigenInterconsulta"
                , c);
        sr.addExtension(origenIC);
        
        String fundamentoPri = HapiFhirUtils.readStringValueFromJsonNode("fundamentoPriorizacion", node);
        
        Extension ext = HapiFhirUtils.buildExtension(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionStringFundamentoPriorizacion"
                , new StringType(fundamentoPri));
        sr.addExtension(ext);
        
        
        ext = HapiFhirUtils.buildExtension(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEstadoInterconsultaCodigoLE", 
                new CodeableConcept(VSEstadoInterconsultaEnum.ESPERA_REFERENCIA.getCoding()));
        
        sr.addExtension(ext);
        
        
        
        
        
        return sr;
    }
  
    
    public void setServiceRequestReferences(ServiceRequest ser,Patient pat,Encounter enc,
        PractitionerRole requester,Condition diagSos,List<AllergyIntolerance> alls,
        Observation indiceComorbilidad,Observation cuidador, Observation dis,
        QuestionnaireResponse motDer,List<ServiceRequest> solExams){
        
        ser.setSubject(new Reference(pat));
        ser.setEncounter(new Reference(enc));
        ser.setRequester(new Reference(requester));
        ser.getSupportingInfo().add(new Reference(diagSos));
        alls.forEach(al -> {
            ser.getSupportingInfo().add(new Reference(al));
        });
        ser.getSupportingInfo().add(new Reference(indiceComorbilidad));
        ser.getSupportingInfo().add(new Reference(cuidador));
        ser.getSupportingInfo().add(new Reference(dis));
        ser.getSupportingInfo().add(new Reference(motDer));
        solExams.forEach(sol -> {
            ser.getSupportingInfo().add(new Reference(sol));
        });
        
    }
    
    public void addResourceToBundle(Bundle b, Resource r){
        IdType id = IdType.newRandomUuid();
        b.addEntry().setFullUrl(id.getIdPart())
                .setResource(r);
    }
}
