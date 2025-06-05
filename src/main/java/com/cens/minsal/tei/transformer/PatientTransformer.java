/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Collections;


/**
 *
 * @author Juan F. <jfanasco@cens.cl>
 */
@Component
public class PatientTransformer {

    static final String PROFILE = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PatientLE";

    public Patient transform( JsonNode node, OperationOutcome oo){

        Patient patient = new Patient();
        patient.getMeta().addProfile(PROFILE);
        patient.getMeta().setLastUpdated(new Date());

        // ID
        //patient.setId(node.get("id").asText());

        // Identificadores
        if(node.get("tipoEvento")!=null && !node.get("tipoEvento").equals("iniciar")){
        JsonNode identificadores = node.get("identificadores");
            if (identificadores.has("RUN")) {
                Identifier id = new Identifier();
                id.setUse(Identifier.IdentifierUse.OFFICIAL);
                id.setSystem("https://interop.minsal.cl/fhir/ig/tei/CodeSystem/CSRut");
                id.setValue(identificadores.get("RUN").asText());
                patient.setIdentifier(Collections.singletonList(id));
            }
        }

        // Nombre
        HumanName nombre = new HumanName();
        nombre.setUse(HumanName.NameUse.OFFICIAL);

        if (node.has("nombreCompleto")) {
            JsonNode nombreCompleto = node.get("nombreCompleto");
            if (nombreCompleto.has("nombres")) {
                for (JsonNode n : nombreCompleto.get("nombres")) {
                    nombre.addGiven(n.asText());
                }
            }
            if (nombreCompleto.has("primerApellido")) {
                nombre.setFamily(nombreCompleto.get("primerApellido").asText());
            }
            if (nombreCompleto.has("segundoApellido")) {
                String primerApellido = nombre.hasFamily() ? nombre.getFamily() : "";
                nombre.setFamily(primerApellido + " " + nombreCompleto.get("segundoApellido").asText());
            }
        }
        patient.setName(Collections.singletonList(nombre));

        // Género
        if (node.has("genero")) {
            String genero = node.get("genero").asText().toLowerCase();
            switch (genero) {
                case "masculino":
                    patient.setGender(Enumerations.AdministrativeGender.MALE);
                    break;
                case "femenino":
                    patient.setGender(Enumerations.AdministrativeGender.FEMALE);
                    break;
                default:
                    patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
            }
        }

        // Fecha de nacimiento
        if (node.has("fechaNacimiento")) {
            try {
                String fechaStr = node.get("fechaNacimiento").asText();
                Date fecha = new SimpleDateFormat("dd-MM-yyyy").parse(fechaStr);
                patient.setBirthDate(fecha);
            } catch (ParseException e) {
                e.printStackTrace(); // Manejo simple de error
            }
        }

        // Dirección
        if (node.has("direccion")) {
            JsonNode direccionNode = node.get("direccion");
            Address direccion = new Address();
            direccion.setUse(Address.AddressUse.HOME);
            direccion.setType(Address.AddressType.PHYSICAL);

            if (direccionNode.has("linea")) {
                direccion.setLine(Collections.singletonList(new StringType(direccionNode.get("linea").asText())));
            }
            if (direccionNode.has("comuna")) {
                String codigo = direccionNode.get("comuna").get("codigo").asText();
                direccion.addExtension(HapiFhirUtils.buildExtension(
                        "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ext-comuna",
                        new CodeType(codigo)
                ));
            }
            if (direccionNode.has("region")) {
                String codigo = direccionNode.get("region").get("codigo").asText();
                direccion.addExtension(HapiFhirUtils.buildExtension(
                        "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ext-region",
                        new CodeType(codigo)
                ));
            }
            if (direccionNode.has("pais")) {
                direccion.setCountry(direccionNode.get("pais").get("codigo").asText());
            }

            patient.setAddress(Collections.singletonList(direccion));
        }

        // Contacto
        if (node.has("contacto")) {
            JsonNode contacto = node.get("contacto");

            if (contacto.has("telefono")) {
                ContactPoint phone = new ContactPoint();
                phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
                phone.setUse(ContactPoint.ContactPointUse.MOBILE);
                phone.setValue(contacto.get("telefono").asText());
                patient.addTelecom(phone);
            }

            if (contacto.has("email")) {
                ContactPoint email = new ContactPoint();
                email.setSystem(ContactPoint.ContactPointSystem.EMAIL);
                email.setUse(ContactPoint.ContactPointUse.HOME);
                email.setValue(contacto.get("email").asText());
                patient.addTelecom(email);
            }
        }

        patient.setId(HapiFhirUtils.readStringValueFromJsonNode("id", node));


        return patient;
    }

    private void addIdentifier(Patient p, String code, String text, JsonNode valueNode, OperationOutcome oo) {
        if (valueNode != null && !valueNode.isNull()) {
            Identifier id = new Identifier();
            id.setUse(Identifier.IdentifierUse.OFFICIAL);
            id.setValue(valueNode.asText());
            id.getType().addCoding().setSystem("https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSTipoIdentificador").setCode(code);
            id.getType().setText(text);
            p.addIdentifier(id);
        } else {
            HapiFhirUtils.addNotFoundIssue(text, oo);
        }
    }


}

