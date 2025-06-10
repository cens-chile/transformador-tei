/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.models.info.Contact;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.List;


/**
 *
 * @author Juan F. <jfanasco@cens.cl>
 */
@Component
public class PatientTransformer {

    static final String PROFILE = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PatientLE";

    ValueSetValidatorService validator;

    public PatientTransformer(ValueSetValidatorService validator) {
        this.validator = validator;
    }


    public Patient transform( JsonNode node, OperationOutcome oo){

        Patient patient = new Patient();
        patient.getMeta().addProfile(PROFILE);
        patient.getMeta().setLastUpdated(new Date());

        // ID
        //patient.setId(node.get("id").asText());

        // Identificadores
        String tipoEvento = HapiFhirUtils.readStringValueFromJsonNode("tipoEvento", node);
        if(tipoEvento == null) HapiFhirUtils.addNotFoundIssue("tipoEvento", oo);
        JsonNode identificadores = node.get("identificadores");
        if(identificadores == null) HapiFhirUtils.addNotFoundIssue("paciente.identificadores", oo);

        if (identificadores.has("RUN")) {
                addIdentifier(patient, "01", "RUN", identificadores.get("RUN"), oo);
            }
        //}

        // Nombre
        HumanName nombre = new HumanName();
        nombre.setUse(HumanName.NameUse.OFFICIAL);

        if (node.has("nombreCompleto")) {
            JsonNode nombreCompleto = node.get("nombreCompleto");
            if (nombreCompleto.has("nombres")) {
                for (JsonNode n : nombreCompleto.get("nombres")) {
                    nombre.addGiven(n.asText());
                }
            } else HapiFhirUtils.addNotFoundIssue("nombres",oo);
            if (nombreCompleto.has("primerApellido")) {
                nombre.setFamily(nombreCompleto.get("primerApellido").asText());
            } else HapiFhirUtils.addNotFoundIssue("primerApellido",oo);
            if (nombreCompleto.has("segundoApellido")) {
                String primerApellido = nombre.hasFamily() ? nombre.getFamily() : "";
                nombre.setFamily(primerApellido + " " + nombreCompleto.get("segundoApellido").asText());
            }
        } else HapiFhirUtils.addNotFoundIssue("nombreCompleto",oo);

        patient.setName(Collections.singletonList(nombre));

        // Género
        if (node.has("codIdGenero")) {
            String genero = node.get("codIdGenero").asText().toLowerCase();
            String valido = validator.validateCode("https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSIdentidaddeGenero",
                    HapiFhirUtils.readStringValueFromJsonNode("codIdGenero", node),"","https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSIdentidaddeGenero");
            if(valido == null) HapiFhirUtils.addInvalidIssue("paciente.codIdGenero",oo);
            Extension extIDGen = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/IdentidadDeGenero",
                    new StringType((genero)));
            patient.addExtension(extIDGen);

        } else HapiFhirUtils.addNotFoundIssue("paciente.codIdGenero",oo);

        if (node.has("sexoBiologico")) {
            String sexoBiologico = node.get("sexoBiologico").asText().toLowerCase();
            String valido = validator.validateCode("http://hl7.org/fhir/administrative-gender",
                    HapiFhirUtils.readStringValueFromJsonNode("sexoBiologico", node),"",
                    "http://hl7.org/fhir/ValueSet/administrative-gender");
            if(valido == null) HapiFhirUtils.addInvalidIssue("paciente.sexoBiologico",oo);
            Extension sexoBioExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/SexoBiologico",
                    new StringType((sexoBiologico)));
            patient.addExtension(sexoBioExt);

        } else HapiFhirUtils.addNotFoundIssue("paciente.codIdGenero",oo);

        if(node.has("nacionalidad")){
            String nacionalidad = HapiFhirUtils.readStringValueFromJsonNode("nacionalidad", node);
            String valido = validator.validateCode("https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CodPais",
                    HapiFhirUtils.readStringValueFromJsonNode("nacionalidad", node),"",
                    "https://hl7chile.cl/fhir/ig/clcore/ValueSet/CodPais");

            if(valido == null) HapiFhirUtils.addInvalidIssue("paciente.nacionalidad",oo);
            Extension nacionalidadExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/CodigoPaises",
                    new StringType((nacionalidad)));
            patient.addExtension(nacionalidadExt);
            }else HapiFhirUtils.addNotFoundIssue("paciente.nacionalidad",oo);

        if(node.has("paisOrigen")){
            String paisOrigen = HapiFhirUtils.readStringValueFromJsonNode("paisOrigen", node);
            String valido = validator.validateCode("https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CodPais",
                    HapiFhirUtils.readStringValueFromJsonNode("paisOrigen", node),"",
                    "https://hl7chile.cl/fhir/ig/clcore/ValueSet/CodPais");

            if(valido == null) HapiFhirUtils.addInvalidIssue("paciente.paisOrigen",oo);
            Extension paisOrigenExt = new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PaisOrigenMPI",
                    new StringType((paisOrigen)));
            patient.addExtension(paisOrigenExt);

        }else HapiFhirUtils.addNotFoundIssue("paciente.paisOrigen",oo);

        //dePuebloOriginario

        if(node.has("dePuebloOriginario")){
            Boolean dePuebloOriginario = HapiFhirUtils.readBooleanValueFromJsonNode("dePuebloOriginario", node);
            Extension dePuebloOriginarioExt = new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PueblosOriginariosPerteneciente",
                    new BooleanType(dePuebloOriginario));
            patient.addExtension(dePuebloOriginarioExt);

        }else HapiFhirUtils.addNotFoundIssue("paciente.dePuebloOriginario",oo);


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
        if (node.has("direcciones")) {
            JsonNode direcciones= node.get("direcciones");
            List<Address> addressList = new ArrayList<>();

            for (JsonNode direccionNode : direcciones){
                Address direccion = new Address();
                if (direccionNode.has("codigoUso")) {
                    String usoDir = HapiFhirUtils.readStringValueFromJsonNode("codigoUso", direccionNode);
                    switch (usoDir) { // lo puse en español, pero en la guia está en inglés (core CL)
                        case "hogar": {
                            direccion.setUse(Address.AddressUse.HOME);
                            break;
                        }
                        case "trabajo": {
                            direccion.setUse(Address.AddressUse.WORK);
                            break;
                        }
                        case "temporal": {
                            direccion.setUse(Address.AddressUse.TEMP);
                            break;
                        }
                        case "antiguo": {
                            direccion.setUse(Address.AddressUse.OLD);
                        break;
                        }
                    }
                    //direccion.setUse(Address.AddressUse.HOME);
                    direccion.setType(Address.AddressType.PHYSICAL);
                }
                if (direccionNode.has("direccion")) {
                    direccion.setLine(Collections.singletonList(new StringType(direccionNode.get("direccion").asText())));
                }
                    if (direccionNode.has("pais")) {
                        direccion.setCountry(direccionNode.get("pais").get("codigo").asText());
                    }

                    if (direccionNode.has("region")) {
                        String codigo = direccionNode.get("region").get("codigo").asText();
                        String valido = validator.validateCode("https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodRegionCL",
                                codigo, "", "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosRegionesCL");
                        if (valido == null) HapiFhirUtils.addInvalidIssue("region.codigo", oo);
                        direccion.addExtension(HapiFhirUtils.buildExtension(
                                "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/RegionesCl",
                                new CodeType(codigo)));
                    }

                    //https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodProvinciasCL

                if (direccionNode.has("provincia")) {
                    String codigo = direccionNode.get("provincia").get("codigo").asText();
                    String valido = validator.validateCode("https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodProvinciasCL",
                            codigo, "", "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosProvinciasCL");
                    if (valido == null) HapiFhirUtils.addInvalidIssue("provincia.codigo", oo);
                    direccion.addExtension(HapiFhirUtils.buildExtension(
                            "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/ProvinciasCl",
                            new CodeType(codigo)));
                }

                    if (direccionNode.has("comuna")) {
                        String codigo = direccionNode.get("comuna").get("codigo").asText();
                        direccion.addExtension(HapiFhirUtils.buildExtension(
                            "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/ComunasCl",
                            new CodeType(codigo)
                    ));
                    }
                    if (direccionNode.has("provincia")) {
                        String codigo = direccionNode.get("provincia").get("codigo").asText();
                        direccion.addExtension(HapiFhirUtils.buildExtension(
                            "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/ProvinciasCl",
                            new CodeType(codigo)
                    ));

                    }

                    if (direccionNode.has("situacionCalle")) {
                    Boolean sitCalleB = HapiFhirUtils.readBooleanValueFromJsonNode("situacionCalle", direccionNode);
                    Extension sitCalleExt =
                            new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/SituacionCalle",
                                    new BooleanType(sitCalleB));
                    direccion.addExtension(sitCalleExt);
                    }

                /*String codigo = direccionNode.get("region").get("codigo").asText();
                String valido = validator.validateCode("https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodRegionCL",
                        codigo, "", "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosRegionesCL");
                if (valido == null) HapiFhirUtils.addInvalidIssue("region.codigo", oo);
                direccion.addExtension(HapiFhirUtils.buildExtension(
                        "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/RegionesCl",
                        new CodeType(codigo)));
                        */

                addressList.add(direccion);
            }
            patient.setAddress(addressList);
        }


        // Contacto
        if (node.has("contacto")) {
            JsonNode contactos = node.get("contacto");
            List<ContactPoint> contactPointList  = new ArrayList<>();
            for (JsonNode contacto: contactos) {
                ContactPoint cp = new ContactPoint();
                if (contacto.has("telefono")) {
                    cp.setSystem(ContactPoint.ContactPointSystem.PHONE);
                    cp.setUse(ContactPoint.ContactPointUse.MOBILE);
                    cp.setValue(contacto.get("telefono").asText());
                    patient.addTelecom(cp);
                } else if (contacto.has("email")) {
                    cp.setSystem(ContactPoint.ContactPointSystem.EMAIL);
                    cp.setUse(ContactPoint.ContactPointUse.HOME);
                    cp.setValue(contacto.get("email").asText());
                    patient.addTelecom(cp);
                } else HapiFhirUtils.addNotFoundIssue("paciente.telefono o email", oo);
                contactPointList.add(cp);
            }
            patient.setTelecom(contactPointList);
            patient.setId(HapiFhirUtils.readStringValueFromJsonNode("id", node));
        }
        else HapiFhirUtils.addNotFoundIssue("paciente.contacto", oo);

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

