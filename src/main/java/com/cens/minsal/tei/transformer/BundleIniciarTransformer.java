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
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
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
    MessageHeaderTransformer messageHeaderTransformer;
    ValueSetValidatorService validator;
    
    public BundleIniciarTransformer(FhirServerConfig fhirServerConfig,
            MessageHeaderTransformer messageHeaderTransformer,
            ValueSetValidatorService validator) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
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

        get = node.get("solicitudIC");
        ServiceRequest sr = null;
        if(get!=null)
            sr = buildServiceRequest(get, out);  
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC", out);

        
        
        //Construir Organización que inicia la IC
        Organization org = null;
        try{
            get = node.get("establecimiento").get("origen");
            if(get!=null)
                org = OrganizationTransformer.transform(get, out,"establecimiento.origen");
        }catch(NullPointerException ex){
            HapiFhirUtils.addNotFoundIssue("establecimientoAPS", out);
        }
        //Contruir Indice Comorbilidad
        Observation buildIndiceComorbilidad  = null;
        if(node.get("indiceComorbilidad")!=null){
           buildIndiceComorbilidad = ObservationTransformer.buildIndiceComporbilidad(node.get("indiceComorbilidad"),out); 
           sr.getSupportingInfo().add(new Reference(buildIndiceComorbilidad));
        }
        
        //Construir Diagnostico
        ConditionTransformer conditionTransformer = new ConditionTransformer(validator);
        Condition cond = null;
        if(node.get("diagnostico")!=null){
            cond = conditionTransformer.transform(node.get("diagnostico"), out,"diagnostico");
        }
        else
            HapiFhirUtils.addNotFoundIssue("diagnostico", out);
            
       
        
        if (!out.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(out,fhirServerConfig.getFhirContext());
            return res;
        }
        
        IdType mHId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(mHId.getIdPart())
                .setResource(messageHeader);
        
        IdType sRId = IdType.newRandomUuid();
        
        b.addEntry().setFullUrl(sRId.getIdPart())
                .setResource(sr);
        
        setMessageHeaderReferences(messageHeader, new Reference(sr), null);
        
        
        IdType iCId = IdType.newRandomUuid();
            b.addEntry().setFullUrl(iCId.getIdPart())
                .setResource(buildIndiceComorbilidad);
            
        IdType orgId = IdType.newRandomUuid();
            b.addEntry().setFullUrl(orgId.getIdPart())
                .setResource(org);
            
            
        IdType condId = IdType.newRandomUuid();
            b.addEntry().setFullUrl(condId.getIdPart())
                .setResource(cond);
        
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
        if(atPreferente!=null){
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
    
    
    
}
