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
public class PractitionerTransformer {



    public Practitioner transform( String profile, JsonNode node, OperationOutcome oo){

        String prestadorPro = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PractitionerProfesionalLE";
        String prestadorAdm = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PractitionerAdministrativoLE";

        Practitioner practitioner = new Practitioner();
        practitioner.getMeta().addProfile(profile);
        practitioner.getMeta().setLastUpdated(new Date());

        // ID

        practitioner.setId(HapiFhirUtils.readStringValueFromJsonNode("id", node));


        String idGen = HapiFhirUtils.readStringValueFromJsonNode("identidadDeGenero", node);

        if(idGen != null){
            Extension idGeneroExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/1.9.2/StructureDefinition-IdentidadDeGenero.html", new StringType(idGen));
            practitioner.addExtension(idGeneroExt);
        }


        if(profile.equals(prestadorPro)) {
            String nacionalidad = HapiFhirUtils.readStringValueFromJsonNode("nacionalidad", node);
            if (idGen != null) {
                Extension nacionalidadExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/1.9.2/StructureDefinition-CodigoPaises.html", new StringType(nacionalidad));
                practitioner.addExtension(nacionalidadExt);
            }
        }

        // Identificadores
        JsonNode identificadores = node.get("identificadores");
        if (identificadores != null) {
            addIdentifier(practitioner, "01", "RUN", identificadores.get("RUN"), oo);
            if(profile.equals(prestadorPro)) {
                addIdentifier(practitioner, "13", "RNPI", identificadores.get("RNPI"), oo);
            }
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
        String genero = HapiFhirUtils.readStringValueFromJsonNode("genero", node);
        if ("masculino".equalsIgnoreCase(genero)) {
            practitioner.setGender(Enumerations.AdministrativeGender.MALE);
        } else if ("femenino".equalsIgnoreCase(genero)) {
            practitioner.setGender(Enumerations.AdministrativeGender.FEMALE);
        } else if ("otro".equalsIgnoreCase(genero)) {
            practitioner.setGender(Enumerations.AdministrativeGender.OTHER);
        } else {
            practitioner.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        }

        // Fecha de nacimiento
        try {
            if(HapiFhirUtils.readDateValueFromJsonNode("fechaNacimiento", node) != null) {
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

        if(profile.equals(prestadorPro)) {
            // Calificaciones (títulos, especialidades, subespecialidades, etc.)
            addQualifications(practitioner, node.get("titulosProfesionales"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSTituloProfesional", "cert");
            addQualifications(practitioner, node.get("especialidadesMedicas"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed", "esp");
            addQualifications(practitioner, node.get("subespecialidadesMedicas"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed", "subesp");
            addQualifications(practitioner, node.get("especialidadesOdontologicas"),  "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadOdont", "EspOdo");
            addQualifications(practitioner, node.get("especialidadesBioquimicas"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadBioqca", "EspBioQ");
            addQualifications(practitioner, node.get("especialidadesFarmacologicas"), "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadFarma", "EspFarma");
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

