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
import java.util.Date;

/**
 *
 * @author Juan F. <jfanasco@cens.cl>
 */
@Component
public class PrestadorProfesionalTransformer {

    private static final String PROFILE = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PractitionerProfesionalLE";

    public Practitioner transform(JsonNode node, OperationOutcome oo) throws ParseException {
        Practitioner practitioner = new Practitioner();
        practitioner.getMeta().addProfile(PROFILE);
        practitioner.getMeta().setLastUpdated(new Date());

        // ID
        practitioner.setId(HapiFhirUtils.readStringValueFromJsonNode("id", node));

        // Identificadores
        JsonNode identificadores = node.get("identificadores");
        if (identificadores != null) {
            addIdentifier(practitioner, "01", "RUN", identificadores.get("RUN"), oo);
            addIdentifier(practitioner, "13", "RNPI", identificadores.get("RNPI"), oo);
        }

        // Activo
        if (HapiFhirUtils.readBooleanValueFromJsonNode("activo", node) != null){
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
        String genero = HapiFhirUtils.readStringValueFromJsonNode("sexo", node);
        if ("masculino".equalsIgnoreCase(genero)) {
            practitioner.setGender(Enumerations.AdministrativeGender.MALE);
        } else if ("femenino".equalsIgnoreCase(genero)) {
            practitioner.setGender(Enumerations.AdministrativeGender.FEMALE);
        } else {
            practitioner.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        }

        // Fecha de nacimiento
        if(HapiFhirUtils.readDateValueFromJsonNode("fechaNacimiento", node) != null) {
            practitioner.setBirthDate(HapiFhirUtils.readDateValueFromJsonNode("fechaNacimiento", node));
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
        JsonNode direccion = node.get("direccion");
        if (direccion != null) {
            Address address = new Address();
            address.setUse(Address.AddressUse.WORK);
            address.addLine(HapiFhirUtils.readStringValueFromJsonNode("linea", direccion));
            address.setCity(HapiFhirUtils.readStringValueFromJsonNode("comuna.nombre", direccion));
            address.setDistrict(HapiFhirUtils.readStringValueFromJsonNode("provincia.nombre", direccion));
            address.setState(HapiFhirUtils.readStringValueFromJsonNode("region.nombre", direccion));
            address.setCountry(HapiFhirUtils.readStringValueFromJsonNode("pais.nombre", direccion));

            // Agregar extensiones de códigos
            HapiFhirUtils.addCodigoExtension(address.getCityElement(), "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/ComunasCl",
                    "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSComunas", direccion, "comuna.codigo");

            HapiFhirUtils.addCodigoExtension(address.getDistrictElement(), "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/ProvinciasCl",
                    "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSProvincias", direccion, "provincia.codigo");

            HapiFhirUtils.addCodigoExtension(address.getStateElement(), "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/RegionesCl",
                    "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSRegiones", direccion, "region.codigo");

            HapiFhirUtils.addCodigoExtension(address.getCountryElement(), "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/CodigoPaises",
                    "urn:iso:std:iso:3166", direccion, "pais.codigo");

            practitioner.addAddress(address);
        }

        // Calificaciones (títulos, especialidades, subespecialidades, etc.)
        addQualifications(practitioner, node.get("titulosProfesionales"), "MEDICO", "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSTitulosProfesionales", "cert");
        addQualifications(practitioner, node.get("especialidadesMedicas"), "CARDIOLOGIA", "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadesMedicas", "esp");
        addQualifications(practitioner, node.get("subespecialidadesMedicas"), "SUBESP", "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSSubespecialidadesMedicas", "subesp");
        addQualifications(practitioner, node.get("especialidadesOdontologicas"), "ODO", "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadesOdontologicas", "odo");
        addQualifications(practitioner, node.get("especialidadesBioquimicas"), "BIOQ", "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadesBioquimicas", "bioq");
        addQualifications(practitioner, node.get("especialidadesFarmacologicas"), "FARMA", "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadesFarmacologicas", "farma");

        // Idiomas
        JsonNode idiomas = node.get("idiomas");
        if (idiomas != null && idiomas.isArray()) {
            for (JsonNode idioma : idiomas) {
                CodeableConcept cc = new CodeableConcept();
                Coding coding = new Coding()
                        .setSystem("urn:ietf:bcp:47")
                        .setCode(HapiFhirUtils.readStringValueFromJsonNode("codigo", idioma))
                        .setDisplay(HapiFhirUtils.readStringValueFromJsonNode("nombre", idioma));
                cc.addCoding(coding);
                practitioner.addCommunication(cc);
            }
        }

        return practitioner;
    }

    private void addIdentifier(Practitioner p, String code, String text, JsonNode valueNode, OperationOutcome oo) {
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

    private void addQualifications(Practitioner p, JsonNode arrayNode, String defaultCode, String system, String identifierValue) {
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode q : arrayNode) {
                Practitioner.PractitionerQualificationComponent qual = new Practitioner.PractitionerQualificationComponent();
                qual.addIdentifier().setValue(identifierValue);
                qual.setCode(new CodeableConcept().addCoding(
                        new Coding()
                                .setSystem(system)
                                .setCode(HapiFhirUtils.readStringValueFromJsonNode("codigo", q))
                                .setDisplay(HapiFhirUtils.readStringValueFromJsonNode("nombre", q))
                ).setText(HapiFhirUtils.readStringValueFromJsonNode("nombre", q)));

                // Periodo
                String start = HapiFhirUtils.readStringValueFromJsonNode("fechaEmision", q);
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

