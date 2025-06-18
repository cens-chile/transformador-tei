/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;

/**
 *
 * @author Juan F. <jfanasco@cens.cl>
 */
@Component
public class PractitionerTransformer {

    ValueSetValidatorService validator;

    public PractitionerTransformer(ValueSetValidatorService validator) {
        this.validator = validator;
    }

    public Practitioner transform( String tipoPractitioner, JsonNode node, OperationOutcome oo) {

        String prestadorPro = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PractitionerProfesionalLE";
        String prestadorAdm = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PractitionerAdministrativoLE";

        Practitioner practitioner = new Practitioner();
        practitioner.getMeta().setLastUpdated(new Date());

        if (tipoPractitioner.equals("administrativo")) {
            practitioner.getMeta().addProfile(prestadorAdm);
        } else {
            practitioner.getMeta().addProfile(prestadorPro);
        }


        // ID

        practitioner.setId(HapiFhirUtils.readStringValueFromJsonNode("id", node));


        if (node.has("identidadGenero")) {
            String genero = HapiFhirUtils.readStringValueFromJsonNode("identidadGenero", node);
            String vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSIdentidaddeGenero";
            String cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSIdentidaddeGenero";
            String valido = validator.validateCode(cs, genero, "", vs);

            if (valido != null) {

                Coding cod = new Coding(cs, genero, valido);
                CodeableConcept cc = new CodeableConcept(cod);
                Extension extIDGen = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/IdentidadDeGenero",
                        cc);
                practitioner.addExtension(extIDGen);
            } else HapiFhirUtils.addNotFoundIssue("paciente.identidadGenero", oo);
        }

        if (tipoPractitioner.equals("profesional")) {
            if(node.has("nacionalidad")){
                String nacionalidad = HapiFhirUtils.readStringValueFromJsonNode("nacionalidad", node);
                String cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CodPais";
                String  vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/CodPais";
                String valido = validator.validateCode(cs,
                        nacionalidad,"",
                        vs);
                if(valido == null) HapiFhirUtils.addInvalidIssue("Prestador.nacionalidad",oo);
                Coding coding = new Coding(cs,nacionalidad,valido);
                CodeableConcept cc = new CodeableConcept(coding);
                Extension nacionalidadExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/CodigoPaises",
                        cc);
                practitioner.addExtension(nacionalidadExt);
            }
        }
        // Identificadores
        JsonNode identificadores = node.get("identificadores");
        if (identificadores != null) {
            addIdentifier(practitioner, "01", "RUN", identificadores.get("RUN"), oo);
            if (tipoPractitioner.equals("profesional")) {
                addIdentifier(practitioner, "13", "RNPI", identificadores.get("RNPI"), oo);
            }
        }


        // Activo
        if (HapiFhirUtils.readBooleanValueFromJsonNode("activo", node) != null) {
            practitioner.setActive(Boolean.TRUE.equals(HapiFhirUtils.readBooleanValueFromJsonNode("activo", node)));
        }

        // Nombre
        JsonNode nombreCompleto = node.get("nombreCompleto");
        if (nombreCompleto != null) {
            HumanName name = new HumanName();
            name.setUse(HumanName.NameUse.OFFICIAL);
            name.setFamily(HapiFhirUtils.readStringValueFromJsonNode("primerApellido", nombreCompleto));

            // Segundo apellido como extensión
            String segundoApellido = HapiFhirUtils.readStringValueFromJsonNode("segundoApellido", nombreCompleto);
            if (segundoApellido != null) {
                Extension segundoApellidoExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/SegundoApellido",
                        new StringType(segundoApellido));
                name.getFamilyElement().addExtension(segundoApellidoExt);
            }

            // Nombres
            JsonNode nombres = nombreCompleto.get("nombres");
            if (nombres != null && nombres.isArray()) {
                for (JsonNode nombre : nombres) {
                    name.addGiven(nombre.asText());
                }
            }
            practitioner.addName(name);
        }

        // Género
        String genero = HapiFhirUtils.readStringValueFromJsonNode("sexoBiologico", node);
        if ("masculino".equalsIgnoreCase(genero) || "male".equalsIgnoreCase(genero) ||
                "hombre".equalsIgnoreCase(genero)) {
            practitioner.setGender(Enumerations.AdministrativeGender.MALE);
        } else if ("femenino".equalsIgnoreCase(genero) || "mujer".equalsIgnoreCase(genero) || "female".equalsIgnoreCase(genero)) {
            practitioner.setGender(Enumerations.AdministrativeGender.FEMALE);
        } else if ("otro".equalsIgnoreCase(genero) || "other".equalsIgnoreCase(genero)) {
            practitioner.setGender(Enumerations.AdministrativeGender.OTHER);
        } else {
            practitioner.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        }

        // Fecha de nacimiento
        try {
            if (HapiFhirUtils.readDateValueFromJsonNode("fechaNacimiento", node) != null) {
                try {
                    practitioner.setBirthDate(HapiFhirUtils.readDateValueFromJsonNode("fechaNacimiento", node));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        // Contacto
        JsonNode contacto = node.get("contacto");
        if (contacto != null) {
            String telefono = HapiFhirUtils.readStringValueFromJsonNode("telefono", contacto);
            if (telefono != null) {
                practitioner.addTelecom(new ContactPoint()
                        .setSystem(ContactPoint.ContactPointSystem.PHONE)
                        .setUse(ContactPoint.ContactPointUse.WORK)
                        .setValue(telefono));
            }

            String email = HapiFhirUtils.readStringValueFromJsonNode("email", contacto);
            if (email != null) {
                practitioner.addTelecom(new ContactPoint()
                        .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                        .setUse(ContactPoint.ContactPointUse.WORK)
                        .setValue(email));
            }
        }

        // Dirección
        JsonNode direccionNode = node.get("direccion");
        if (direccionNode != null) {
            Address direccion = new Address();
            if (direccionNode.has("descripcion")) {
                direccion.setLine(Collections.singletonList(new StringType(direccionNode.get("descripcion").asText())));
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
            practitioner.addAddress(direccion);
        }


        if (tipoPractitioner.equals("profesional")) {
            JsonNode tits = node.get("titulosProfesionales");
            if (tits != null) {
                addQualifications(practitioner, node.get("titulosProfesionales"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSTituloProfesional", "cert");
            }
            if (tipoPractitioner.equals("profesional")) {
                // Calificaciones (títulos, especialidades, subespecialidades, etc.)
                addQualifications(practitioner, node.get("especialidadesMedicas"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed", "esp");
                addQualifications(practitioner, node.get("subespecialidadesMedicas"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed", "subesp");
                addQualifications(practitioner, node.get("especialidadesOdontologicas"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadOdont", "EspOdo");
                addQualifications(practitioner, node.get("especialidadesBioquimicas"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadBioqca", "EspBioQ");
                addQualifications(practitioner, node.get("especialidadesFarmacologicas"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadFarma", "EspFarma");
            }
        }
        return practitioner;
    }

    private void addIdentifier(Practitioner p, String code, String text, JsonNode valueNode, OperationOutcome oo) {
        if (valueNode != null && !valueNode.isNull()) {
            Identifier id = new Identifier();
            id.setUse(Identifier.IdentifierUse.OFFICIAL);
            id.setValue(valueNode.asText());
            id.getType().addCoding().setSystem("https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSTipoIdentificador").setCode(code).setDisplay(text);
            id.getType().setText(text);
            p.addIdentifier(id);
        } else {
            HapiFhirUtils.addNotFoundIssue(text, oo);
        }
    }

    private void addQualifications(Practitioner p, JsonNode node, String system, String identifierValue) {
        if (node != null && node.isArray()) {
            for (JsonNode q : node) {
                Practitioner.PractitionerQualificationComponent qual = new Practitioner.PractitionerQualificationComponent();
                qual.addIdentifier().setValue(identifierValue);
                qual.setCode(new CodeableConcept().addCoding(
                        new Coding()
                                .setSystem(system)
                                .setCode(HapiFhirUtils.readStringValueFromJsonNode("codigo", q))
                                .setDisplay(HapiFhirUtils.readStringValueFromJsonNode("nombre", q))
                ).setText(HapiFhirUtils.readStringValueFromJsonNode("nombre", q)));

                // Periodo
                String start = null;
                try {
                    start = HapiFhirUtils.transformarFecha(HapiFhirUtils.readStringValueFromJsonNode("fechaEmision", q));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }

                if (start != null) {
                    Period period = new Period();
                    period.setStartElement(new DateTimeType(start));
                    qual.setPeriod(period);
                }

                // Institución emisora
                String issuer = HapiFhirUtils.readStringValueFromJsonNode("institucion", q);
                if (issuer != null) {
                    qual.setIssuer(new Reference().setDisplay(issuer));
                }

                p.addQualification(qual);
            }
        }
    }
}

