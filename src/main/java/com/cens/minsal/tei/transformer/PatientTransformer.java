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
        boolean identicadoresValid = HapiFhirUtils.validateArrayInJsonNode("paciente.identificadores", identificadores,oo,true);
        boolean existeRun = false;
        if (identicadoresValid) {
            for (JsonNode identificador : identificadores) {

                String code = HapiFhirUtils.readStringValueFromJsonNode("codigo", identificador);
                if (code == null)
                    HapiFhirUtils.addNotFoundIssue("paciente.identificadores.codigo", oo);

                String valor = HapiFhirUtils.readStringValueFromJsonNode("valor", identificador);

                if (valor == null) {
                    HapiFhirUtils.addNotFoundIssue("paciente.identificadores.valor", oo);
                }
                Identifier identifier = new Identifier();
                cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSTipoIdentificador";
                vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSTipoIdentificador";
                valido = validator.validateCode(cs, code, "", vs);
                if (valido.equalsIgnoreCase("RUN")) existeRun = true;
                if (valido == null) HapiFhirUtils.addNotFoundCodeIssue("paciente.identificadores.codigo", oo);
                identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
                if (valor == null)
                    HapiFhirUtils.addNotFoundIssue("paciente.identificadores.valor", oo);
                identifier.setValue(valor);
                Coding codingIdentifier = new Coding(cs, code, valido);
                identifier.getType().addCoding(codingIdentifier);
                identifier.setValue(valor);


                String paisEmision = HapiFhirUtils.readStringValueFromJsonNode("paisEmision", identificador);
                vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/CodPais";
                cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CodPais";
                valido = validator.validateCode(cs, paisEmision, "", vs);
                if (valido != null) {
                    identifier.setAssigner(new Reference().setDisplay(valido));
                    Coding coding = new Coding(cs, paisEmision, valido);
                    CodeableConcept cc = new CodeableConcept(coding);
                    Extension paisEmisionExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/CodigoPaises", cc);
                    identifier.getType().addExtension(paisEmisionExt);
                } else HapiFhirUtils.addNotFoundCodeIssue("paciente.identificacion.paisEmision", oo);
                //identifier.getType().setText(tipo);
                patient.addIdentifier(identifier);

            }
        }
        if(!existeRun)HapiFhirUtils.addErrorIssue("paciente.identificadores", "No existe identificador tipo RUN", oo);
        // Nombre
        HumanName nombre = new HumanName();
        nombre.setUse(HumanName.NameUse.OFFICIAL);


            JsonNode nombreCompleto = node.get("nombreCompleto");
            boolean nombreValid = HapiFhirUtils.validateObjectInJsonNode("nombreCompleto",nombreCompleto,oo,true);
            if(nombreCompleto != null) {
                if (nombreCompleto.has("nombres")) {
                    HapiFhirUtils.validateArrayInJsonNode("paciente.nombreCompleto.nombres",
                            nombreCompleto.get("nombres"), oo, true);
                    int i = 0;
                    for (JsonNode n : nombreCompleto.get("nombres")) {
                        if (n != null && !n.isNull()) {
                            String valor = n.asText();
                            if (!valor.isEmpty()) {
                                    if (!valor.isEmpty()) {
                                        nombre.addGiven(valor);
                                        i++;
                                    }
                                }
                        }
                    }
                    if (i==0){
                        HapiFhirUtils.addNotFoundIssue("paciente.nombre",oo);
                    }
                }
                if (nombreCompleto.has("primerApellido")) {
                    nombre.setFamily(HapiFhirUtils.readStringValueFromJsonNode("primerApellido", nombreCompleto));
                    if (nombreCompleto.has("segundoApellido")) {
                        nombre.getFamilyElement().addExtension(new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/SegundoApellido",
                                new StringType(HapiFhirUtils.readStringValueFromJsonNode("segundoApellido", nombreCompleto))));
                    }
                }else
                    HapiFhirUtils.addNotFoundIssue("paciente.nombreCompleto.primerApellido", oo);

                patient.addName(nombre);
            }

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

            JsonNode fallecimiento = node.get("fallecimiento");
            boolean fallecimientoValid = HapiFhirUtils.validateObjectInJsonNode("paciente.fallecimiento", fallecimiento,oo,true);
            if(fallecimientoValid) {
                if (fallecimiento.has("fallecido")) {
                    boolean fallecido = HapiFhirUtils.readBooleanValueFromJsonNode("fallecido", fallecimiento);
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
                    HapiFhirUtils.addNotFoundIssue("Paciente.fallecimiento.fallecido", oo);
                }
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
                 patient.setGender(Enumerations.AdministrativeGender.fromCode(sexoBiologico));

             }else HapiFhirUtils.addNotFoundCodeIssue("paciente.sexoBiologico",oo);

        } else HapiFhirUtils.addNotFoundIssue("paciente.sexoBiologico", oo);

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
        else
            HapiFhirUtils.addNotFoundIssue("paciente.nacionalidad",oo);

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
            if(!node.get("pueblosOriginariosPerteneciente").isBoolean()) HapiFhirUtils.addErrorIssue("paciente.pueblosOriginariosPerteneciente", "debe ser booleano", oo);
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

        if(node.has("puebloAfroPertenencia")){
            Boolean puebloAfroPertenencia = HapiFhirUtils.readBooleanValueFromJsonNode("puebloAfroPertenencia", node);
            Extension puebloAfroPertenenciaExt = new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PueblosAfrodescendiente",
                    new BooleanType(puebloAfroPertenencia));
            patient.addExtension(puebloAfroPertenenciaExt);

        }

        if (node.has("fechaNacimiento")) {
            try {
                Date fecha = HapiFhirUtils.readDateValueFromJsonNode("fechaNacimiento", node);
                patient.setBirthDate(fecha);
            } catch (ParseException e) {
                HapiFhirUtils.addErrorIssue("paciente.fechaNacimiento", "Error al procesar paciente.fechaNacimiento",oo);
                //e.printStackTrace(); // Manejo simple de error
            }
        } else HapiFhirUtils.addNotFoundIssue("paciente.fechaNacimiento", oo);

        // Dirección
        if (node.has("direcciones")) {
            JsonNode direcciones= node.get("direcciones");
            HapiFhirUtils.validateArrayInJsonNode("Paciente.direcciones",direcciones,oo,false);
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
                     JsonNode pais = direccionNode.get("pais");
                     HapiFhirUtils.validateObjectInJsonNode("paciente.direcciones.pais", pais, oo,false);
                     String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", direccionNode.get("pais"));
                     valido = validator.validateCode(cs,codigo,"",vs);
                     if (valido != null){
                         Coding coding = new Coding(cs,codigo,valido);
                         CodeableConcept cc = new CodeableConcept(coding);
                         direccion.getCountryElement().addExtension(HapiFhirUtils.buildExtension(
                                "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/CodigoPaises",cc));
                     }else
                         HapiFhirUtils.addInvalidIssue("paciente.direcciones.codigo", oo);
                }

                if (direccionNode.has("region")) {
                    JsonNode region = direccionNode.get("region");
                    HapiFhirUtils.validateObjectInJsonNode("paciente.direcciones.region", region,oo, false);
                    String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", direccionNode.get("region"));
                     vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosRegionesCL";
                     cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodRegionCL";
                     valido = validator.validateCode(cs, codigo, "", vs);
                    if (valido != null){
                    Coding coding = new Coding(cs,codigo,valido);
                    CodeableConcept cc = new CodeableConcept(coding);
                    direccion.getStateElement().addExtension(HapiFhirUtils.buildExtension(
                            "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/RegionesCl",cc));
                    } else  HapiFhirUtils.addNotFoundCodeIssue("Paciente.direccion.region.codigo", oo);
                }


                if (direccionNode.has("provincia")) {
                    JsonNode provincia = direccionNode.get("provincia");
                    HapiFhirUtils.validateObjectInJsonNode("paciente.direcciones.provincia", provincia, oo, false);
                    String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", direccionNode.get("provincia"));
                    vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosProvinciasCL";
                    cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodProvinciasCL";
                    valido = validator.validateCode(cs, codigo, "", vs);
                    if (valido != null) {
                        Coding coding = new Coding(cs, codigo, valido);
                        CodeableConcept cc = new CodeableConcept(coding);
                        direccion.getDistrictElement().addExtension(HapiFhirUtils.buildExtension(
                                "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/ProvinciasCl", cc));
                    } else HapiFhirUtils.addNotFoundCodeIssue("Paciente.direccion.provincia.codigo", oo);
                }

                if (direccionNode.has("comuna")) {
                    JsonNode comunaJ = direccionNode.get("comuna");
                    HapiFhirUtils.validateObjectInJsonNode("paciente.direcciones.comuna", comunaJ,oo, false);
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
            JsonNode contactos = node.get("contacto");
        boolean contactosValid = HapiFhirUtils.validateArrayInJsonNode("paciente.contacto", contactos,oo,true);
        List<ContactPoint> contactPointList  = new ArrayList<>();
        int conteoContactos = 0;
        if(contactosValid) {
            int i=0;
            for (JsonNode contacto : contactos) {
                i++;
                ContactPoint cp = new ContactPoint();
                if (contacto.has("sistemaDeContacto") && contacto.has("valorContacto")) {
                    String sistemaDeContacto = HapiFhirUtils.readStringValueFromJsonNode("sistemaDeContacto", contacto);
                    if(sistemaDeContacto == null)
                        HapiFhirUtils.addErrorIssue("paciente.contacto.sistemaDeContacto", "nulo o vacío", oo);
                    String valorContacto = HapiFhirUtils.readStringValueFromJsonNode("valorContacto", contacto);
                    if(valorContacto == null)
                        HapiFhirUtils.addErrorIssue("paciente.contacto.valorContacto", "nulo o vacío", oo);

                    switch (sistemaDeContacto) {
                        case "phone":
                            cp.setSystem(ContactPoint.ContactPointSystem.PHONE);
                            cp.setValue(valorContacto);
                            conteoContactos++;
                            contactPointList.add(cp);
                            break;

                        case "email":
                            cp.setSystem(ContactPoint.ContactPointSystem.EMAIL);
                            cp.setValue(valorContacto);
                            conteoContactos++;
                            contactPointList.add(cp);
                            break;

                        default:
                            HapiFhirUtils.addInvalidIssue("paciente.contacto.sistemaDeContacto (permitido email y phone)", oo);
                            break;
                    }
                }else {
                    if(!contacto.has("sistemaDeContacto")){
                        HapiFhirUtils.addNotFoundIssue("paciente.contacto["+i+"].sistemaDeContacto", oo);
                    }
                    if(!contacto.has("valorContacto")){
                        HapiFhirUtils.addNotFoundIssue("paciente.contacto["+i+"].valorContacto", oo);
                    }
                }
        }
                if (contactPointList.size() > 0) {
                    patient.setTelecom(contactPointList);
                } else HapiFhirUtils.addNotFoundIssue("paciente.contacto o email", oo);

        }

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

