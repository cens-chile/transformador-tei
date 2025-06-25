/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import co.elastic.clients.util.DateTime;
import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.models.info.Contact;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender;
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

        String vs =""; String cs =""; String valido = "";

        JsonNode identificadores = node.get("identificadores");
        if(identificadores == null) HapiFhirUtils.addNotFoundIssue("paciente.identificadores", oo);
        for (JsonNode identificador: identificadores){
            String code = HapiFhirUtils.readStringValueFromJsonNode("codigo",identificador);
            String valor = HapiFhirUtils.readStringValueFromJsonNode("valor",identificador);
            String tipo = HapiFhirUtils.readStringValueFromJsonNode("tipo", identificador);
            Identifier identifier = new Identifier();
            cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSTipoIdentificador";
            vs =  "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSTipoIdentificador";
                identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
                identifier.setValue(valor);
            valido =validator.validateCode(cs,code,"",vs);
            if(valido == null) HapiFhirUtils.addNotFoundCodeIssue("paciente.identificadores.codigo",oo);

            identifier.getType().addCoding().setSystem(cs).setCode(code).setDisplay(valido);

            String paisEmision = HapiFhirUtils.readStringValueFromJsonNode("paisEmision", identificador);
            vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/CodPais";
            cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CodPais";
            valido = validator.validateCode(cs,paisEmision,"",vs);
            if (valido != null) {
                Coding coding = new Coding(cs, paisEmision, valido);
                CodeableConcept cc = new CodeableConcept(coding);
                Extension paisEmisionExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/CodigoPaises", cc);
                identifier.getType().addExtension(paisEmisionExt);
            }else HapiFhirUtils.addNotFoundCodeIssue("paciente.identificacion.paisEmision",oo);
            identifier.getType().setText(tipo);
            patient.addIdentifier(identifier);

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
            } else HapiFhirUtils.addNotFoundIssue("nombres",oo);
            if (nombreCompleto.has("primerApellido")) {
                nombre.setFamily(nombreCompleto.get("primerApellido").asText());
            } else HapiFhirUtils.addNotFoundIssue("primerApellido",oo);
            if (nombreCompleto.has("segundoApellido")) {
                String primerApellido = nombre.hasFamily() ? nombre.getFamily() : "";
                nombre.setFamily(primerApellido + " " + nombreCompleto.get("segundoApellido").asText());
            }
        } else HapiFhirUtils.addNotFoundIssue("nombreCompleto",oo);

        patient.addName(nombre);

        if(node.has("nombreSocial")){
            String nombreSocial = HapiFhirUtils.readStringValueFromJsonNode("nombreSocial",node);
                HumanName nombreSocialHN = new HumanName();
                nombreSocialHN.setUse(HumanName.NameUse.USUAL);
                nombreSocialHN.addGiven(nombreSocial);
                patient.addName(nombreSocialHN);
        }

        // Género
        if (node.has("identidadGenero")) {
            String genero = HapiFhirUtils.readStringValueFromJsonNode("identidadGenero", node);
            vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSIdentidaddeGenero";
            cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSIdentidaddeGenero";
            valido = validator.validateCode(cs, genero, "", vs);

            if (valido != null) {
                Coding cod = new Coding(cs, genero, valido);
                CodeableConcept cc = new CodeableConcept(cod);
                Extension extIDGen = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/IdentidadDeGenero",
                        cc);
                patient.addExtension(extIDGen);
            } else HapiFhirUtils.addNotFoundCodeIssue("paciente.identidadGenero", oo);
        } else HapiFhirUtils.addNotFoundIssue("paciente.identidadGenero", oo);

        if(node.has("estadoCivil")){
            String ec = HapiFhirUtils.readStringValueFromJsonNode("estadoCivil", node);
            //********Validar el estado civil
             vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEstadoCivil";
             cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEstadoCivil";
            String valid = validator.validateCode(cs,ec,"",vs);
            if(valid != null) {
                Coding coding = new Coding(cs, ec, valid);
                patient.setMaritalStatus(new CodeableConcept(coding));
            }else HapiFhirUtils.addErrorIssue(ec, "codigo de estadoCivil no valido", oo);
        }

        if (node.has("fallecimiento")) {
            JsonNode fallecimiento = node.get("fallecimiento");

            if (fallecimiento != null && fallecimiento.isObject()) {

                if (fallecimiento.has("fallecido")) {
                    JsonNode fallecidoNode = fallecimiento.get("fallecido");

                    if (fallecidoNode != null && fallecidoNode.isBoolean()) {
                        boolean fallecido = fallecidoNode.booleanValue();
                        patient.setDeceased(new BooleanType(fallecido));

                        if (fallecido && fallecimiento.has("fechaFallecimiento")) {
                            try {
                                Date fechaFallecimiento = HapiFhirUtils.readDateValueFromJsonNode("fechaFallecimiento", fallecimiento);
                                patient.setDeceased(new DateTimeType(fechaFallecimiento));
                            } catch (Exception e) {
                                HapiFhirUtils.addErrorIssue("fechaFallecimiento", "fecha de fallecimiento no válida", oo);
                            }
                        }

                    } else {
                        HapiFhirUtils.addErrorIssue("Paciente.fallecimiento.fallecido", "debe ser Booleano", oo);
                    }

                } else {
                    HapiFhirUtils.addNotFoundIssue("Paciente.fallecimiento.fallecido", oo);
                }

            } else {
                HapiFhirUtils.addErrorIssue("paciente.Fallecimiento", "debe ser un objeto JSON", oo);
            }

        } else {
            HapiFhirUtils.addNotFoundIssue("paciente.Fallecimiento", oo);
        }
        if(node.has("religion")){
            String religion  = HapiFhirUtils.readStringValueFromJsonNode("religion", node);
             vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSReligion";
             cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSReligion";
             valido = validator.validateCode(cs,religion,"",vs);
            if (valido != null){
                Coding code = new Coding(cs,religion,valido);
                CodeableConcept cc = new CodeableConcept(code);
                Extension religionExt = new Extension(
                        "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/Religion",cc );
                patient.addExtension(religionExt);
            }
        }

        if(node.has("sexoRegistral")){
            vs = "http://hl7.org/fhir/ValueSet/administrative-gender";
            cs = "http://hl7.org/fhir/administrative-gender";
            String code = HapiFhirUtils.readStringValueFromJsonNode("sexoRegistral", node);
             valido = validator.validateCode(cs,code,"",vs);
            if(valido != null) {
                    patient.setGender(Enumerations.AdministrativeGender.fromCode(code));
            } else HapiFhirUtils.addNotFoundCodeIssue("paciente.sexoRegistral",oo);
        }else HapiFhirUtils.addNotFoundIssue("paciente.sexoRegistral",oo);

        if (node.has("sexoBiologico")) {
            String sexoBiologico = HapiFhirUtils.readStringValueFromJsonNode("sexoBiologico",node);
             cs = "http://hl7.org/fhir/administrative-gender";
            vs = "http://hl7.org/fhir/ValueSet/administrative-gender";
             valido = validator.validateCode(cs,
                    HapiFhirUtils.readStringValueFromJsonNode("sexoBiologico", node),"",vs);
             if(valido != null) {
                 Coding coding = new Coding(cs, sexoBiologico, valido);
                 CodeableConcept cc = new CodeableConcept(coding);
                 Extension sexoBioExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/SexoBiologico",
                         cc);
                 patient.addExtension(sexoBioExt);

             }else HapiFhirUtils.addNotFoundCodeIssue("paciente.sexoBiologico",oo);

        }

        if(node.has("nacionalidad")){
            String nacionalidad = HapiFhirUtils.readStringValueFromJsonNode("nacionalidad", node);
            cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CodPais";
            vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/CodPais";
             valido = validator.validateCode(cs,nacionalidad,"", vs);
            if(valido == null) HapiFhirUtils.addInvalidIssue("paciente.nacionalidad",oo);
            Coding coding = new Coding(cs,nacionalidad,valido);
            CodeableConcept cc = new CodeableConcept(coding);
            Extension nacionalidadExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/CodigoPaises",
                    cc);
            patient.addExtension(nacionalidadExt);
        }
        else HapiFhirUtils.addNotFoundIssue("paciente.nacionalidad",oo);

        if(node.has("paisOrigen")){
            String paisOrigen = HapiFhirUtils.readStringValueFromJsonNode("paisOrigen", node);
            cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CodPais";
            vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/CodPais";
            valido = validator.validateCode(cs,
                    paisOrigen,"",
                    vs);

            if(valido == null) HapiFhirUtils.addNotFoundCodeIssue("paciente.paisOrigen",oo);
            Coding coding = new Coding(cs,paisOrigen,valido);
            CodeableConcept cc = new CodeableConcept(coding);
            Extension paisOrigenExt = new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PaisOrigenMPI",
                    cc);
            patient.addExtension(paisOrigenExt);
        }else HapiFhirUtils.addNotFoundIssue("paciente.paisOrigen",oo);

        if(node.has("pueblosOriginariosPerteneciente")){
            Boolean pueblosOriginariosPerteneciente = HapiFhirUtils.readBooleanValueFromJsonNode("pueblosOriginariosPerteneciente", node);
            Extension dePuebloOriginarioExt = new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PueblosOriginariosPerteneciente",
                    new BooleanType(pueblosOriginariosPerteneciente));
            patient.addExtension(dePuebloOriginarioExt);

            if(pueblosOriginariosPerteneciente && node.has("pueblosOriginarios")){
                String pueblosOriginarios = HapiFhirUtils.readStringValueFromJsonNode("pueblosOriginarios", node);
                 cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/PueblosOriginariosCS";
                 vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/PueblosOriginariosVS";
                 valido = validator.validateCode(cs,pueblosOriginarios,"",vs);
                if (valido != null) {
                    Coding coding = new Coding(cs, pueblosOriginarios, valido);
                    CodeableConcept cc = new CodeableConcept(coding);
                    if(pueblosOriginarios.equals(10)){
                        if (node.has("otroPuebloOriginario")){
                            String otroPuebloOriginario = HapiFhirUtils.readStringValueFromJsonNode(
                                        "otroPuebloOriginario",node);
                            cc.setText(otroPuebloOriginario);
                        }
                    }
                    Extension pOExt =
                            new Extension
                                    ("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PueblosOriginarios",cc);
                    patient.addExtension(pOExt);
                }
            }

        }else HapiFhirUtils.addNotFoundIssue("paciente.pueblosOriginariosPerteneciente",oo);


//puebloAfroPertenencia

        if(node.has("puebloAfroPertenencia")){
            Boolean puebloAfroPertenencia = HapiFhirUtils.readBooleanValueFromJsonNode("puebloAfroPertenencia", node);
            Extension puebloAfroPertenenciaExt = new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PueblosAfrodescendiente",
                    new BooleanType(puebloAfroPertenencia));
            patient.addExtension(puebloAfroPertenenciaExt);

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
        } else HapiFhirUtils.addNotFoundIssue("paciente.fechaNacimiento", oo);

        // Dirección
        if (node.has("direcciones")) {
            JsonNode direcciones= node.get("direcciones");
            List<Address> addressList = new ArrayList<>();

            for (JsonNode direccionNode : direcciones){
                Address direccion = new Address();
                if (direccionNode.has("codigoUso")) {
                    String usoDir = HapiFhirUtils.readStringValueFromJsonNode("codigoUso", direccionNode);
                    boolean uso = false;
                    switch (usoDir) { // lo puse en español, pero en la guia está en inglés (core CL)
                        case "home": {
                            direccion.setUse(Address.AddressUse.HOME);
                            uso=true;
                            break;
                        }
                        case "work": {
                            direccion.setUse(Address.AddressUse.WORK);
                            uso=true;

                            break;
                        }
                        case "temp": {
                            direccion.setUse(Address.AddressUse.TEMP);
                            uso=true;
                            break;
                        }
                        case "old": {
                            direccion.setUse(Address.AddressUse.OLD);
                            uso=true;
                            break;
                        }

                    }
                    if(!uso){
                        HapiFhirUtils.addNotFoundCodeIssue("Paciente.Direccion.codigoUso", oo);
                    }
                    direccion.setType(Address.AddressType.PHYSICAL);
                }
                if (direccionNode.has("direccion")) {
                    direccion.setLine(Collections.singletonList(new StringType(
                            HapiFhirUtils.readStringValueFromJsonNode("direccion",direccionNode))));
                } else  HapiFhirUtils.addNotFoundIssue("Paciente.direccion.direccion", oo);

                if (direccionNode.has("pais")) {
                     vs ="https://hl7chile.cl/fhir/ig/clcore/ValueSet/CodPais";
                     cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CodPais";
                    String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", direccionNode.get("pais"));
                     valido = validator.validateCode(cs,codigo,"",vs);
                    Coding coding = new Coding(cs,codigo,valido);
                    CodeableConcept cc = new CodeableConcept(coding);
                    if (valido != null){
                        direccion.getCountryElement().addExtension(HapiFhirUtils.buildExtension(
                                "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/CodigoPaises",cc));

                    }
                }

                if (direccionNode.has("region")) {
                    String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", direccionNode.get("region"));
                     vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosRegionesCL";
                     cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodRegionCL";
                     valido = validator.validateCode(cs, codigo, "", vs);
                    if (valido == null) HapiFhirUtils.addNotFoundCodeIssue("Paciente.direccion.region.codigo", oo);
                    Coding coding = new Coding(cs,codigo,valido);
                    CodeableConcept cc = new CodeableConcept(coding);
                    direccion.getStateElement().addExtension(HapiFhirUtils.buildExtension(
                            "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/RegionesCl",cc));
                }

                if (direccionNode.has("provincia")) {
                    String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", direccionNode.get("provincia"));
                     vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosProvinciasCL";
                     cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodProvinciasCL";
                     valido = validator.validateCode(cs, codigo, "", vs);
                    if (valido == null) HapiFhirUtils.addNotFoundCodeIssue("Paciente.direccion.provincia.codigo", oo);
                    Coding coding = new Coding(cs,codigo,valido);
                    CodeableConcept cc = new CodeableConcept(coding);
                    direccion.getDistrictElement().addExtension(HapiFhirUtils.buildExtension(
                            "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/ProvinciasCl",cc));
                }

                if (direccionNode.has("comuna")) {
                    JsonNode comunaJ = direccionNode.get("comuna");
                    String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo",comunaJ);
                     vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosComunaCL";
                     cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodComunasCL";

                     valido = validator.validateCode(cs,codigo,"",vs);
                    if (valido != null){
                        Coding coding = new Coding(cs,codigo,valido);
                        CodeableConcept cc = new CodeableConcept(coding);
                        direccion.getCityElement().addExtension(HapiFhirUtils.buildExtension(
                                "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/ComunasCl",
                                cc));
                    } else HapiFhirUtils.addNotFoundCodeIssue("Paciente.direccion.comuna.codigo", oo);
                }

                    if (direccionNode.has("situacionCalle")) {
                    Boolean sitCalleB = HapiFhirUtils.readBooleanValueFromJsonNode("situacionCalle", direccionNode);
                    Extension sitCalleExt =
                            new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/SituacionCalle",
                                    new BooleanType(sitCalleB));
                    direccion.addExtension(sitCalleExt);
                    }

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

