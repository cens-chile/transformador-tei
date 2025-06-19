/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;
import com.cens.minsal.tei.services.ValueSetValidatorService;

import java.util.Date;

/**
 *
 * @author Juan F. <jfanasco@cens.cl>
 */
@Component
public class PractitionerRoleTransformer {
    ValueSetValidatorService validator;

    String profile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PractitionerRoleLE";
    public PractitionerRoleTransformer(ValueSetValidatorService validator){
        this.validator = validator;
    }

    public PractitionerRole transform(JsonNode inputNode,OperationOutcome oo) {
        PractitionerRole role = new PractitionerRole();

        role.getMeta().addProfile(profile);

        String id = HapiFhirUtils.readStringValueFromJsonNode("ID", inputNode);
        if (id != null) {
            role.setId(id);
        }

        Boolean activo = HapiFhirUtils.readBooleanValueFromJsonNode("Activo", inputNode);
        if (activo != null) {
            role.setActive(activo);
        }

        JsonNode periodo = inputNode.get("Periodo");
        if (periodo != null) {
            Period period = new Period();
            try {
                Date start = HapiFhirUtils.readDateTimeValueFromJsonNode("Inicio", periodo, "yyyy-MM-dd HH:mm:ss");
                Date end = HapiFhirUtils.readDateTimeValueFromJsonNode("Fin", periodo, "yyyy-MM-dd HH:mm:ss");
                if (start != null) period.setStart(start);
                if (end != null) period.setEnd(end);

            }catch (Exception e){
                HapiFhirUtils.addErrorIssue("Rol de profesional","Fecha de inicio y/o fecha de fin", oo);
            }

            role.setPeriod(period);
        }

        JsonNode prestador = inputNode.get("Prestador");
        if (prestador != null && prestador.has("Referencia")) {
            role.setPractitioner(new Reference(prestador.get("Referencia").asText()));
        }

        JsonNode rol = inputNode.get("Rol");
        if (rol != null) {
            CodeableConcept concept = new CodeableConcept();
            String sistema = rol.has("Sistema") ? rol.get("Sistema").asText() : null;
            String codigo = rol.has("Codigo") ? rol.get("Codigo").asText() : null;



            if (sistema != null && codigo != null) {
                concept.addCoding(new Coding(sistema, codigo, null));
                role.addCode(concept);
            }
        }

        // 7. Especialidades
        JsonNode especialidades = inputNode.get("Especialidades");
        if (especialidades != null && especialidades.isArray()) {
            for (JsonNode esp : especialidades) {
                String sistema = esp.has("Sistema") ? esp.get("Sistema").asText() : null;
                String codigo = esp.has("Codigo") ? esp.get("Codigo").asText() : null;
                if (sistema != null && codigo != null) {
                    CodeableConcept concept = new CodeableConcept();
                    concept.addCoding(new Coding(sistema, codigo, null));
                    role.addSpecialty(concept);
                }
            }
        }

        // 8. Organización
        String orgRef = HapiFhirUtils.readStringValueFromJsonNode("Organizacion", inputNode);
        if (orgRef != null) {
            role.setOrganization(new Reference(orgRef));
        }

        // 9. Ubicaciones
        JsonNode ubicaciones = inputNode.get("Ubicaciones");
        if (ubicaciones != null && ubicaciones.isArray()) {
            for (JsonNode loc : ubicaciones) {
                if (loc.has("ubicacion")) {
                    role.addLocation(new Reference(loc.get("ubicacion").asText()));
                }
            }
        }

        return role;
    }
    
    public PractitionerRole buildPractitionerRole(String role,Organization org,
            Practitioner prac){
        
        PractitionerRole practitionerRole = new PractitionerRole();
        practitionerRole.getMeta().addProfile(profile);
        String valido = validator.validateCode(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSPractitionerTipoRolLE",role,
                "","https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSPractitionerTipoRolLE");
        Coding roleCode = new Coding(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSPractitionerTipoRolLE",role,valido);
        CodeableConcept cc = new CodeableConcept(roleCode);
        cc.setText(role);
        practitionerRole.addCode(cc);
        practitionerRole.setPractitioner(new Reference(prac));
        practitionerRole.setOrganization(new Reference(org));
        return practitionerRole;
    }


}

