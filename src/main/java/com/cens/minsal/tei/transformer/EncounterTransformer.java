/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import com.cens.minsal.tei.services.ValueSetValidatorService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class EncounterTransformer {
    ValueSetValidatorService validator;

    public EncounterTransformer(ValueSetValidatorService validator){
        this.validator = validator;
    }

    public Encounter transform(JsonNode node, OperationOutcome oo, String evento) {
        JsonNode json;
        String encKey;
        if(evento.equals("iniciar"))
            encKey = "encuentroIniciarAPS";
        else
            encKey = "encuentroAtender";
            
        
        json = node.get(encKey);
        if(json==null){
            HapiFhirUtils.addNotFoundIssue(encKey, oo);
            return null;
        }
            
        
        
        
        Encounter encounter = new Encounter();

        ObjectMapper mapper = new ObjectMapper();

        

        // Estado no es necesario solicitarlo , es un dato estático

        encounter.setStatus(EncounterStatus.fromCode("finished"));

        /*
        if (json.has("estado")) {
            JsonNode estado = json.get("estado");
            String cod = null;
            if (estado.has("codigoEstado")) {

                String cs = "http://hl7.org/fhir/encounter-status";
                String vs = "http://hl7.org/fhir/ValueSet/encounter-status";
                cod = HapiFhirUtils.readStringValueFromJsonNode("codigoEstado", estado);
                String resValidacionDest = validator.validateCode(cs,
                        cod, "", vs);

                if (resValidacionDest == null){
                    HapiFhirUtils.addErrorIssue(cod,"encuentro.estado.codigoEstado no valido", oo);
                }


                try {
                    encounter.setStatus(EncounterStatus.fromCode(estado.get("codigoEstado").asText()));
                } catch (Exception e) {
                    HapiFhirUtils.addErrorIssue("Estado inválido: " + estado.get("codigoEstado").asText(), "codigoEstado",oo);
                }
            }
        }else HapiFhirUtils.addNotFoundIssue("estado.codigoEstado", oo);

        */

        // Clase (modalidadAtencion)
        if (json.has("codigoModalidadAtencion")) {
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSModalidadAtencionCodigo";
            String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSModalidadAtencionCodigo";

            Coding classCoding = new Coding();
            String cod = json.get("codigoModalidadAtencion").asText();
            String resValidacionDest = validator.validateCode(cs,
                    cod, "", vs);
            if (resValidacionDest == null){
                HapiFhirUtils.addErrorIssue(cod,"Encuentro.codigoModalidadAtencion", oo);
            }
            classCoding.setCode(cod);
            classCoding.setSystem(cs);
            classCoding.setDisplay(resValidacionDest);
            encounter.setClass_(classCoding);
        } else HapiFhirUtils.addNotFoundIssue( "Encuentro.codigoModalidadAtencion", oo);


        // Tipo de consulta (type)
        if (json.has("tipoDeConsulta")) {
            JsonNode tipoConsulta = json.get("tipoDeConsulta");
            CodeableConcept typeConcept = new CodeableConcept();
            Coding coding = new Coding();
            coding.setCode(String.valueOf(tipoConsulta.get("codigoTipoConsulta").asInt()));
            coding.setSystem(tipoConsulta.get("urlTipoConsulta").asText());
            typeConcept.addCoding(coding);
            encounter.addType(typeConcept);
        }

        // Tipo de servicio (serviceType)
        if (json.has("tipoDeServicio")) {
            JsonNode servicio = json.get("tipoDeServicio");
            CodeableConcept serviceType = new CodeableConcept();
            serviceType.addCoding(new Coding()
                    .setCode(servicio.get("codigo").asText())
                    .setSystem(servicio.get("urlTipoServicio").asText()));
            encounter.setServiceType(serviceType);
        }

        // Cita médica
        if (json.has("citaMedica")) {
            encounter.setAppointment(List.of(new Reference(json.get("citaMedica").get("referenciaACitaMedica").asText())));
        }



        // Período
        if (json.has("periodo")) {
            JsonNode periodo = json.get("periodo");
            try {
                Period period = new Period();
                period.setStart(HapiFhirUtils.readDateTimeValueFromJsonNode( "fechaInicio",periodo,"dd-MM-yyyy HH:mm:SS"));
                period.setEnd(HapiFhirUtils.readDateTimeValueFromJsonNode( "fechaFin",periodo,"dd-MM-yyyy HH:mm:SS"));
                encounter.setPeriod(period);
            } catch (Exception e) {
                HapiFhirUtils.addErrorIssue("Error al parsear fechas del período.", "periodo", oo);
            }
        } else HapiFhirUtils.addNotFoundIssue("periodo", oo);

               // Razones del encuentro
        if (json.has("codigoRazonDelEncuentro")) {
            for (JsonNode razon : json.get("codigoRazonDelEncuentro")) {
                CodeableConcept reason = new CodeableConcept();
                reason.addCoding(new Coding()
                        .setCode(razon.get("codigoRazon").asText())
                        .setSystem(razon.get("urlRazon").asText()));
                encounter.addReasonCode(reason);
            }
        }

        
        if (evento.equals("atender")) {
            encounter.getMeta().addProfile("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/EncounterAtenderLE");
            EncounterTransformer.atenderComplete(json, encounter, oo);
        }
        if (evento.equals("iniciar")) {
            encounter.getMeta().addProfile("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/EncounterIniciarLE");
            iniciarComplete(encounter,json, oo);
        }
        return encounter;
    }


    private static void atenderComplete (JsonNode node, Encounter encounter, OperationOutcome oo){
        
        
        // Identificador
        String iden = HapiFhirUtils.readStringValueFromJsonNode("identificadorConsultaEspecialidad", node);
        if (iden!=null) {
            encounter.getIdentifierFirstRep().setValue(iden);
        } else HapiFhirUtils.addNotFoundIssue("identificadorConsultaEspecialidad" , oo);
        
        // agregar pertenencia y motivo de no pertenencia
        // Pertinencia y motivoNoPertinencia (como extensiones)
        if (node.has("pertinencia")) {
            Extension extPertinencia = HapiFhirUtils.buildExtension(
                    "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionPertinenciaAtencionBox",
                    new BooleanType(node.get("pertinencia").asBoolean()));
            encounter.addExtension(extPertinencia);
        }

        if (node.has("motivoNoPertinencia")) {
            Extension extMotivo = HapiFhirUtils.buildExtension(
                    "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionMotivoNoPertinencia",
                    new StringType(node.get("motivoNoPertinencia").asText()));
            encounter.addExtension(extMotivo);
        }

    }
    
    
    public void iniciarComplete(Encounter enc, JsonNode node, OperationOutcome oo){
        
        String id = HapiFhirUtils.readStringValueFromJsonNode("IdentificacionConsultaAPS", node);
        if(id!=null){
            enc.getIdentifierFirstRep().setValue(id);
        }
        String consecSystem="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSConsecuenciaAtencionCodigo";
        Coding cod = new Coding(consecSystem,"3","Derivación");
        HapiFhirUtils.
                buildExtension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionConsecuenciaAtencionCodigo"
                        , cod);
        
    }
}
