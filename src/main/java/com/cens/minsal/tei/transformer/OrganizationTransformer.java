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
public class OrganizationTransformer {
    public static Organization transform(JsonNode input, OperationOutcome oo)  {
        Organization org = new Organization();

        // ID temporal
        String id = HapiFhirUtils.readStringValueFromJsonNode("ID", input);
        if (id != null) {
            org.setId(id);
        }

        // Meta: perfil OrganizationLE
        org.getMeta().addProfile("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/OrganizationLE");

        // Identificador (Código DEIS)
        String identificador = HapiFhirUtils.readStringValueFromJsonNode("identificador", input);
        String sistema = HapiFhirUtils.readStringValueFromJsonNode("sistema", input);
        if (identificador != null) {
            Identifier idf = new Identifier();
            idf.setSystem(sistema);
            idf.setValue(identificador);
            org.addIdentifier(idf);
        }

        // Activo
        Boolean activo = HapiFhirUtils.readBooleanValueFromJsonNode("activo", input);
        if (activo != null) {
            org.setActive(activo);
        }

        // Nombre legal
        String nombreLegal = HapiFhirUtils.readStringValueFromJsonNode("nombreLegal", input);
        if (nombreLegal != null) {
            org.setName(nombreLegal);
        }

        // Nombres de fantasía (alias)
        JsonNode fantasias = input.get("nombresFantasia");
        if (fantasias != null && fantasias.isArray()) {
            for (JsonNode f : fantasias) {
                org.addAlias(f.asText());
            }
        }

        // Contactos
        JsonNode contactos = input.get("Contactos");
        if (contactos != null && contactos.isArray()) {
            for (JsonNode contacto : contactos) {
                ContactPoint cp = new ContactPoint();
                cp.setSystem(ContactPoint.ContactPointSystem.fromCode(contacto.get("sistema").asText()));
                cp.setValue(contacto.get("valor").asText());
                cp.setUse(ContactPoint.ContactPointUse.fromCode(contacto.get("uso").asText()));
                org.addTelecom(cp);
            }
        }

        // Direcciones
        JsonNode direcciones = input.get("Direcciones");
        if (direcciones != null && direcciones.isArray()) {
            for (JsonNode dir : direcciones) {
                Address address = new Address();
                address.addLine(dir.get("direccion").asText());
                address.setCountry(dir.get("pais").asText());

                // Añadir extensiones codificadas a campos
                HapiFhirUtils.addCodigoExtension(address, "ciudad",
                        "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ComunasCl",
                        "https://datos.gob.cl/codeSystem/comunas", dir.get("comuna").asText(), dir.get("comuna").asText());

                HapiFhirUtils.addCodigoExtension(address, "provincia",
                        "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ProvinciasCl",
                        "https://datos.gob.cl/codeSystem/provincias", dir.get("provincia").asText(), dir.get("provincia").asText());

                HapiFhirUtils.addCodigoExtension(address, "region",
                        "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/RegionesCl",
                        "https://datos.gob.cl/codeSystem/regiones", dir.get("region").asText(), dir.get("region").asText());

                HapiFhirUtils.addCodigoExtension(address, "pais",
                        "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/PaisesCl",
                        "urn:iso:std:iso:3166", dir.get("pais").asText(), dir.get("pais").asText());

                org.addAddress(address);
            }
        }

        // Contactos específicos
        JsonNode contactosEsp = input.get("ContactosEspecificos");
        if (contactosEsp != null && contactosEsp.isArray()) {
            for (JsonNode ce : contactosEsp) {
                Organization.OrganizationContactComponent occ = new Organization.OrganizationContactComponent();

                // Propósito
                JsonNode proposito = ce.get("Proposito");
                if (proposito != null && proposito.has("Codificacion")) {
                    JsonNode codificaciones = proposito.get("Codificacion");
                    if (codificaciones.isArray()) {
                        for (JsonNode cod : codificaciones) {
                            CodeableConcept cc = new CodeableConcept();
                            Coding coding = new Coding();
                            coding.setSystem(cod.get("Sistema").asText());
                            coding.setCode(cod.get("Codigo").asText());
                            cc.addCoding(coding);
                            occ.setPurpose(cc);
                        }
                    }
                }

                // Nombre asociado
                String nombreAsociado = HapiFhirUtils.readStringValueFromJsonNode("NombreAsociado", ce);
                if (nombreAsociado != null) {
                    HumanName nombre = new HumanName();
                    nombre.setText(nombreAsociado);
                    occ.setName(nombre);
                }

                // Detalles de contacto
                JsonNode detalles = ce.get("DetallesContacto");
                if (detalles != null && detalles.isArray()) {
                    for (JsonNode dc : detalles) {
                        ContactPoint cp = new ContactPoint();
                        cp.setSystem(ContactPoint.ContactPointSystem.fromCode(dc.get("sistema").asText()));
                        cp.setValue(dc.get("valor").asText());
                        cp.setUse(ContactPoint.ContactPointUse.fromCode(dc.get("uso").asText()));
                        occ.addTelecom(cp);
                    }
                }

                org.addContact(occ);
            }
        }

        return org;
    }
}

