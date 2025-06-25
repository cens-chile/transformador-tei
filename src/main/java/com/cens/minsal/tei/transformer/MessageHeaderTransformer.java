/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSMessageHeaderEventEnum;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.UriType;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class MessageHeaderTransformer {
    private static final String profile = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/MessageHeaderLE";
    ValueSetValidatorService validator;

    public MessageHeaderTransformer(ValueSetValidatorService validator) {
        this.validator = validator;
    }
    public MessageHeader transform(JsonNode node, OperationOutcome oo){
        MessageHeader m = new MessageHeader();
        m.getMeta().addProfile(profile);
        m.getMeta().setLastUpdated(new Date());
        String tipoEvento = HapiFhirUtils.readStringValueFromJsonNode("tipoEvento",node);
        if(tipoEvento == null) HapiFhirUtils.addNotFoundIssue("tipoEvento", oo);
        String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSTipoEventoLE";
        String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSTipoEventoLE";
        String resValidacion = validator.validateCode(cs, tipoEvento, "", vs);
        if (resValidacion == null){
            HapiFhirUtils.addErrorIssue("Tipo de evento No válido", "--", oo);
        }
        switch (tipoEvento.toLowerCase()) {
            case "iniciar":
                m.setEvent(VSMessageHeaderEventEnum.INICIAR.getCoding());
                break;
            case "referenciar":
                m.setEvent(VSMessageHeaderEventEnum.REFERENCIAR.getCoding());
                break;
            case "revisar":
                m.setEvent(VSMessageHeaderEventEnum.REVISAR.getCoding());
                break;
            case "priorizar":
                m.setEvent(VSMessageHeaderEventEnum.PRIORIZAR.getCoding());
                break;
            case "agendar":
                m.setEvent(VSMessageHeaderEventEnum.AGENDAR.getCoding());
                break;
            case "atender":
                m.setEvent(VSMessageHeaderEventEnum.ATENDER.getCoding());
                break;
            case "terminar":
                m.setEvent(VSMessageHeaderEventEnum.TERMINAR.getCoding());
                break;
            default:
                throw new IllegalArgumentException("Evento desconocido: " + tipoEvento);
        }

        String software = HapiFhirUtils.readStringValueFromJsonNode("software", node);
        if(software!=null)
            m.getSource().setSoftware(software);
        else 
            HapiFhirUtils.addNotFoundIssue("datosSistema.software", oo);
        
        String endpoint = HapiFhirUtils.readStringValueFromJsonNode("endpoint", node);
        if(endpoint!=null){
            try {
                URI uri = new URI(endpoint);
                m.getSource().setEndpoint(endpoint);
            } catch (URISyntaxException ex) {
                HapiFhirUtils.addInvalidIssue("datosSistema.endpoint", oo);
            }
        }
        else 
            HapiFhirUtils.addNotFoundIssue("datosSistema.endpoint", oo);
         
        return m;
    }
    
    
}
