/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.utils.JsonUniqueKeyValidator;
import com.cens.minsal.tei.valuesets.VSEstadoInterconsultaEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IIdType;
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
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class BundleReferenciarTransformer {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BundleReferenciarTransformer.class);
    FhirServerConfig fhirServerConfig;
    static final String bundleProfile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/BundleReferenciarLE";
    static final String snomedSystem = "http://snomed.info/sct";
    PatientTransformer patientTr;
    MessageHeaderTransformer messageHeaderTransformer;
    EncounterTransformer encTransformer;
    OrganizationTransformer orgTransformer;
    AllergyIntoleranceTransformer allInTransformer;
    QuestionnaireResponseTransformer questTransformer;
    ServiceRequestTransformer serTransformer;
    PractitionerTransformer praTransformer;
    PractitionerRoleTransformer referenciadorTransformer;
    ValueSetValidatorService validator;
    
    public BundleReferenciarTransformer(FhirServerConfig fhirServerConfig,
            MessageHeaderTransformer messageHeaderTransformer,
            EncounterTransformer encTransformer,
            PatientTransformer patientTr,
            OrganizationTransformer orgTransformer,
            ValueSetValidatorService validator,
            QuestionnaireResponseTransformer questTransformer,
            AllergyIntoleranceTransformer allInTransformer,
            ServiceRequestTransformer serTransformer,
            PractitionerTransformer praTransformer,
            PractitionerRoleTransformer referenciadorTransformer) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
        this.encTransformer = encTransformer;
        this.patientTr = patientTr;
        this.orgTransformer = orgTransformer;
        this.allInTransformer = allInTransformer;
        this.questTransformer = questTransformer;
        this.serTransformer = serTransformer;
        this.praTransformer = praTransformer;
        this.referenciadorTransformer = referenciadorTransformer;
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
            JsonUniqueKeyValidator.validateUniqueKeys(cmd, out);
            node = mapper.readTree(cmd);

        } catch (JsonProcessingException ex) {
            java.util.logging.Logger.getLogger(BundleReferenciarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }
        
        JsonNode get = node.get("datosSistema");
        MessageHeader messageHeader = null;
        ((ObjectNode)get).put("tipoEvento", "referenciar");

        
        if(get!=null)
            messageHeader = 
                messageHeaderTransformer.transform(node.get("datosSistema"), out);
        else
            HapiFhirUtils.addNotFoundIssue("datosSistema", out);

        String refPat = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente", node);
        if(refPat==null)
            HapiFhirUtils.addNotFoundIssue("referenciaPaciente", out);
            
        get = node.get("prestadorReferenciador");
        Practitioner practitioner = null;
        if(get!=null){
            String tipoPres = HapiFhirUtils.readStringValueFromJsonNode("tipoPrestador", get);
            String[] arr = {"profesional","administrativo"};
            if(tipoPres!=null && Arrays.stream(arr).anyMatch(tipoPres::equals))
                practitioner = praTransformer.transform(tipoPres, get, out);
            else
                HapiFhirUtils.addNotFoundIssue("prestadorReferenciador.tipoPrestador", out);
        }else
            HapiFhirUtils.addNotFoundIssue("prestadorReferenciador", out);
        
        get = node.get("solicitudIC");
        ServiceRequest sr = null;
        if(get!=null)
            sr = buildServiceRequest(get, out);  
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC", out);

        //Construir Organización de origen
        Organization org = null;
        try{
            get = node.get("establecimiento").get("origen");
            if(get!=null)
                org = orgTransformer.transform(get, out,"establecimiento.origen");   
        }catch(NullPointerException ex){
            HapiFhirUtils.addNotFoundIssue("establecimiento.origen", out);
        }
        
        //Construir Organización de destino
        Organization orgDest = null;
        try{
            get = node.get("establecimiento").get("destino");
            if(get!=null)
                orgDest = orgTransformer.transform(get, out,"establecimiento.destino");   
        }catch(NullPointerException ex){
            HapiFhirUtils.addNotFoundIssue("establecimiento.destino", out);
        }
        
        
        
        if (!out.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(out,fhirServerConfig.getFhirContext());
            return res;
        }

        //practitioner.setId("PractitionerReferenciador");
        PractitionerRole referenciador = referenciadorTransformer.buildPractitionerRole("referenciador", org, practitioner);
        PractitionerRole resolutor = referenciadorTransformer.buildPractitionerRole("atendedor", orgDest, null);
        
        HapiFhirUtils.addResourceToBundle(b, messageHeader);
        setMessageHeaderReferences(messageHeader, new Reference(sr), new Reference(referenciador));
        
       
        String srFullUrl = HapiFhirUtils.getUrlBaseFullUrl()+"/ServiceRequest/"+sr.getId();
        HapiFhirUtils.addResourceToBundle(b, sr,srFullUrl);
        sr.getSubject().setReference(refPat);
        sr.getPerformer().add(new Reference(resolutor));
        
        
        HapiFhirUtils.addResourceToBundle(b, practitioner);
        
 
        //Organizacion de origen
        HapiFhirUtils.addResourceToBundle(b, org);
        //Organizacion de Destino
        HapiFhirUtils.addResourceToBundle(b, orgDest);
        
        
        HapiFhirUtils.addResourceToBundle(b,referenciador);
        HapiFhirUtils.addResourceToBundle(b,resolutor);
        
        
        res = HapiFhirUtils.resourceToPrettyString(b, fhirServerConfig.getFhirContext());

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
        
        String id = HapiFhirUtils.readStringValueFromJsonNode("idSolicitudServicio", node);
        if(id!=null)
            sr.setId(id);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC.idSolicitudServicio", oo);
        
        String iden = HapiFhirUtils.readStringValueFromJsonNode("idInterconsulta", node);
        if(iden!=null)
            sr.getIdentifierFirstRep().setValue(iden);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC.idInterconsulta", oo);

        try {
            Date d = HapiFhirUtils.readDateValueFromJsonNode("fechaSolicitudIC", node);
            sr.setAuthoredOn(d);
        } catch (ParseException ex) {
            Logger.getLogger(BundleIniciarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            HapiFhirUtils.addErrorIssue("fechaSolicitudIC", ex.getMessage(), oo);
        }


        sr.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        
       
        String modalidadAtencion = HapiFhirUtils.readStringValueFromJsonNode("modalidadAtencion", node);
        if(modalidadAtencion!=null){
            //VSModalidadAtencionEnum fromCode = VSModalidadAtencionEnum.fromCode(modalidadAtencion);
            String cs ="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSModalidadAtencionCodigo";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSModalidadAtencionCodigo";
            String validateCode = validator.validateCode(cs, modalidadAtencion, null, vs);
            if(validateCode!=null){
                Coding c = new Coding(cs,modalidadAtencion,validateCode);
                sr.getCategoryFirstRep().addCoding(c);
            }
            else
                HapiFhirUtils.addInvalidIssue("solicitudIC.modalidadAtencion", oo);
        }
        
        String destinoAtencion = HapiFhirUtils.readStringValueFromJsonNode("destinoAtencion", node);
        if(destinoAtencion!=null){
            String cs ="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSDestinoReferenciaCodigo";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSDestinoReferenciaCodigo";
            String validateCode = validator.validateCode(cs, destinoAtencion, null, vs);
            if(validateCode!=null){
                Coding c = new Coding(cs,destinoAtencion,validateCode);
                sr.getLocationCodeFirstRep().addCoding(c);
            }
            else
                HapiFhirUtils.addInvalidIssue("solicitudIC.destinoAtencion", oo);
        }
        else
             HapiFhirUtils.addNotFoundIssue("solicitudIC.destinoAtencion", oo);
       
        
        Boolean resolutividadAPS = HapiFhirUtils.readBooleanValueFromJsonNode("resolutividadAPS", node);
        if(resolutividadAPS!=null){
            if(!node.get("resolutividadAPS").isBoolean())
                HapiFhirUtils.addInvalidIssue("solicitudIC.resolutividadAPS", oo);
            
            Extension extResolutividadAPS = 
                HapiFhirUtils.buildBooleanExt(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionBoolResolutividadAPS",
                 resolutividadAPS);
            sr.addExtension(extResolutividadAPS);
            
        }
        
        Extension ext = HapiFhirUtils.buildExtension(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEstadoInterconsultaCodigoLE", 
                new CodeableConcept(VSEstadoInterconsultaEnum.ESPERA_REVISION.getCoding()));    
        sr.addExtension(ext);
        
        //EspecialidadMédicaDestinoCódigo
        JsonNode especialidad = node.get("especialidadMedicaDestino");
        if(especialidad!=null){
            String cs ="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VsEspecialidadDest";
            String get = HapiFhirUtils.readStringValueFromJsonNode("tipo", especialidad);
            if(get!=null){
                if(get.equals("odontologica"))
                    cs="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadOdont";
                else if(!get.equals("medica"))
                    HapiFhirUtils.addErrorIssue("solicitudIC.especialidadMedicaDestino.tipo",
                            "No se conoce el tipo de especialidad", oo);
            }     
            get = HapiFhirUtils.readStringValueFromJsonNode("codigo", especialidad);
            if(get!=null){
                String validateCode = validator.validateCode(cs, get, null, vs);
                if(validateCode!=null){
                    String extUrl ="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEspecialidadMedicaDestinoCodigo";
                    Coding c = new Coding(cs,get,validateCode);
                    Extension buildExtension = HapiFhirUtils.buildExtension(extUrl,new CodeableConcept(c));
                    sr.getExtension().add(buildExtension);
                }
                else
                    HapiFhirUtils.addNotFoundCodeIssue("solicitudIC.especialidadMedicaDestino.codigo", oo);
            }else
                HapiFhirUtils.addNotFoundIssue("solicitudIC.especialidadMedicaDestino.codigo", oo);
        }
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC.especialidadMedicaDestino", oo);
        
        //EspecialidadMédicaDestinoCódigo
        JsonNode subEspecialidad = node.get("subEspecialidadMedicaDestino");
        if(subEspecialidad!=null){
            String cs ="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VsEspecialidadDest";
            String get = HapiFhirUtils.readStringValueFromJsonNode("tipo", subEspecialidad);
            if(get!=null){
                if(get.equals("odontologica"))
                    cs="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadOdont";
                else if(!get.equals("medica"))
                    HapiFhirUtils.addErrorIssue("solicitudIC.subEspecialidadMedicaDestino.tipo",
                            "No se conoce el tipo de especialidad", oo);
            }     
            get = HapiFhirUtils.readStringValueFromJsonNode("codigo", subEspecialidad);
            if(get!=null){
                String validateCode = validator.validateCode(cs, get, null, vs);
                if(validateCode!=null){
                    String extUrl ="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionSubEspecialidadMedicaDestinoCodigo";
                    Coding c = new Coding(cs,get,validateCode);
                    Extension buildExtension = HapiFhirUtils.buildExtension(extUrl,new CodeableConcept(c));
                    sr.getExtension().add(buildExtension);
                }
                else
                    HapiFhirUtils.addNotFoundCodeIssue("solicitudIC.subEspecialidadMedicaDestino.codigo", oo);
            }else
                HapiFhirUtils.addNotFoundIssue("solicitudIC.subEspecialidadMedicaDestino.codigo", oo);
        }
        
        
        
        
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
