/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSMessageHeaderEventEnum;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Date;

import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class MessageHeaderTransformer {
    private static final String profile = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/MessageHeaderLE";
    
    
    public MessageHeader transform(JsonNode node, OperationOutcome oo){
        MessageHeader m = new MessageHeader();
        m.getMeta().addProfile(profile);
        m.getMeta().setLastUpdated(new Date());
        
        m.setEvent(VSMessageHeaderEventEnum.TERMINAR.getCoding());
        
        String software = HapiFhirUtils.readStringValueFromJsonNode("software", node);
        if(software!=null)
            m.getSource().setSoftware(software);
        else 
            HapiFhirUtils.addNotFoundIssue("software", oo);
        
        String endpoint = HapiFhirUtils.readStringValueFromJsonNode("endpoint", node);
        if(software!=null)
            m.getSource().setEndpoint(endpoint);
        else 
            HapiFhirUtils.addNotFoundIssue("endpoint", oo);
         
        return m;
    }
    
    
}
