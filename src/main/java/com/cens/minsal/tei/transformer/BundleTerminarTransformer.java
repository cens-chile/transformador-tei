/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.ssn.fhir.tei.valuesets.VSModalidadAtencionEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class BundleIniciarTransformer {
    
    FhirServerConfig fhirServerConfig;
    static final String bundleProfile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/BundleIniciarLE";
    
    MessageHeaderTransformer messageHeaderTransformer;
    
    
    public BundleIniciarTransformer(FhirServerConfig fhirServerConfig,
            MessageHeaderTransformer messageHeaderTransformer) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
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
            String toString = node.toString();

        } catch (JsonProcessingException ex) {
            java.util.logging.Logger.getLogger(BundleIniciarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }
        
        JsonNode get = node.get("datosSistema");
        MessageHeader messageHeader = null;
        if(get!=null)
            messageHeader = 
                messageHeaderTransformer.coreDataSetTEIToMessageHeader(node.get("datosSistema"), out);
        else
            HapiFhirUtils.addNotFoundIssue("datosSistema", out);

        get = node.get("solicitudIC");
        ServiceRequest sr = null;
        if(get!=null)
            sr = buildServiceRequest(get, out);  
        else
             HapiFhirUtils.addNotFoundIssue("solicitudIC", out);

        
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
        String moadalidadAtencion = HapiFhirUtils.readStringValueFromJsonNode("modalidadAtencion", node);
        Coding coding = VSModalidadAtencionEnum.fromCode(moadalidadAtencion).getCoding();
        sr.getCategoryFirstRep().addCoding(coding);
        return sr;
    }
}
