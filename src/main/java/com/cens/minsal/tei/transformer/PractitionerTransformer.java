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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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

        switch (tipoPractitioner){
        case "administrativo":
        practitioner.getMeta().addProfile(prestadorAdm);
            break;
        case "profesional":
            practitioner.getMeta().addProfile(prestadorPro);
            break;
        default: HapiFhirUtils.addNotFoundCodeIssue("Prestador.tipoPrestador",oo);
        }



        // ID

        //practitioner.setId(HapiFhirUtils.readStringValueFromJsonNode("id", node));

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
        if(identificadores!= null) {
            Boolean validoB = HapiFhirUtils.validateObjectInJsonNode("Prestador.identificadores", identificadores, oo, true);
            if (!validoB) HapiFhirUtils.addNotFoundIssue("Prestador.identificadores", oo);
            String run = HapiFhirUtils.readStringValueFromJsonNode("RUN", identificadores);

            String rnpi = HapiFhirUtils.readStringValueFromJsonNode("RNPI", identificadores);
            if (run != null && !identificadores.get("RUN").isTextual()) {
                HapiFhirUtils.addErrorIssue("Prestador.Identificadores.RUN",
                        "Si existe, Debe ser texto(string)", oo);
            }

            if (identificadores.has("RNPI")) {
                try {
                    boolean a = identificadores.get("RNPI").isTextual();
                    if(!a) HapiFhirUtils.addErrorIssue("prestador.RNPI", "debe ser texto", oo);
                } catch (Exception e) {
                    HapiFhirUtils.addErrorIssue("Prestador.Identificadores.RNPI",
                            "Si existe, Debe ser texto(string)", oo);
                    throw new RuntimeException(e);
                }
            }

            if (identificadores.has("RUN")) {
                try {
                    boolean a = identificadores.get("RUN").isTextual();
                    if(!a) HapiFhirUtils.addErrorIssue("prestador.RUN", "debe ser texto", oo);
                } catch (Exception e) {
                    HapiFhirUtils.addErrorIssue("prestador.identificadores.RUN",
                            "Si existe, Debe ser texto(string)", oo);
                    throw new RuntimeException(e);
                }
            }


            if (run == null && rnpi == null)
                HapiFhirUtils.addErrorIssue("RNPI y RUN", "Debe existir al menos uno de los 2 identificadores", oo);

            if (run != null) {
                addIdentifier(practitioner, "01", "RUN", identificadores.get("RUN"), oo);
            }else {
                HapiFhirUtils.addNotFoundIssue("prestador.RUN",oo);
            }

            if (tipoPractitioner.equals("profesional") && rnpi != null) {
                addIdentifier(practitioner, "13", "RNPI", identificadores.get("RNPI"), oo);
            }
        } else HapiFhirUtils.addNotFoundIssue("prestador.identificadores", oo);
        practitioner.setActive(true);

        // Nombre
        JsonNode nombreCompleto = node.get("nombreCompleto");
        Boolean valido = HapiFhirUtils.validateObjectInJsonNode("Practitioner.nombreCompleto", nombreCompleto,oo,true);

        if (valido) {
            HumanName name = new HumanName();
            name.setUse(HumanName.NameUse.OFFICIAL);
            String apellido = HapiFhirUtils.readStringValueFromJsonNode("primerApellido", nombreCompleto);
            if(apellido != null && nombreCompleto.get("primerApellido").isTextual()) {
                name.setFamily(apellido);
            }else HapiFhirUtils.addNotFoundIssue("Prestador.nombreCompleto.primerApellido", oo);
            // Segundo apellido como extensión
            String segundoApellido = HapiFhirUtils.readStringValueFromJsonNode("segundoApellido", nombreCompleto);
            if (segundoApellido != null) {
                Extension segundoApellidoExt = new Extension("https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/SegundoApellido",
                        new StringType(segundoApellido));
                name.getFamilyElement().addExtension(segundoApellidoExt);
            }

            // Nombres
            JsonNode nombres = nombreCompleto.get("nombres");
            Boolean validNombres = HapiFhirUtils.validateArrayInJsonNode("prestador.nombrecompleto.nombres", nombres,oo,true);
            if (validNombres) {
                for (JsonNode nombre : nombres) {
                    name.addGiven(nombre.asText());
                }
            } else HapiFhirUtils.addNotFoundIssue("Prestador.nombrecompleto.nombres", oo);

            practitioner.addName(name);
        }



        if(node.has("sexoRegistral")){
            String vs = "http://hl7.org/fhir/ValueSet/administrative-gender";
            String cs = "http://hl7.org/fhir/administrative-gender";
            String code = HapiFhirUtils.readStringValueFromJsonNode("sexoRegistral", node);
            String validoS = validator.validateCode(cs,code,"",vs);
            if(validoS != null) {
                practitioner.setGender(Enumerations.AdministrativeGender.fromCode(code));
            } else HapiFhirUtils.addNotFoundCodeIssue("prestador.sexoRegistral",oo);
        }



        try {
            if (HapiFhirUtils.readDateValueFromJsonNode("fechaNacimiento", node) != null) {
                try {
                    practitioner.setBirthDate(HapiFhirUtils.readDateValueFromJsonNode("fechaNacimiento", node));
                } catch (ParseException e) {
                    HapiFhirUtils.addErrorIssue("Prestador.fechaNacimiento", "error de formato de fecha", oo);
                    throw new RuntimeException(e);
                }
            } else HapiFhirUtils.addNotFoundIssue("prestador.fechaNacimiento", oo);
        } catch (ParseException e) {
            HapiFhirUtils.addErrorIssue("Prestador.fechaNacimiento", "error de formato de fecha", oo);
            throw new RuntimeException(e);
        }

        // Contacto
        JsonNode contactos = node.get("contacto");
        boolean contactosValid = HapiFhirUtils.validateArrayInJsonNode("prestador.contacto", contactos,oo,false);
        List<ContactPoint> contactPointList  = new ArrayList<>();
        int conteoContactos = 0;
        if(contactosValid) {
            for (JsonNode contacto : contactos) {
                ContactPoint cp = new ContactPoint();
                if (contacto.has("tipoDeContacto") && contacto.has("valorContacto")) {
                    String tipoDeContacto = HapiFhirUtils.readStringValueFromJsonNode("tipoDeContacto", contacto);
                    if(tipoDeContacto == null)
                        HapiFhirUtils.addErrorIssue("prestador.contacto.tipoDeContacto", "nulo o vacío", oo);
                    String valorContacto = HapiFhirUtils.readStringValueFromJsonNode("valorContacto", contacto);
                    if(valorContacto == null)
                        HapiFhirUtils.addErrorIssue("prestador.contacto.valorContacto", "nulo o vacío", oo);

                    switch (tipoDeContacto) {
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
                            HapiFhirUtils.addInvalidIssue("prestador.contacto.tipoDeContacto (permitido email y phone)", oo);
                            break;
                    }
                }else HapiFhirUtils.addNotFoundIssue("prestador.contacto.tipoDeContacto & prestador.contacto.valorContacto", oo);
            }
            if (contactPointList.size() > 0) {
                practitioner.setTelecom(contactPointList);
            }
        }

        // Dirección
        JsonNode direccionNode = node.get("direccion");
        if (direccionNode != null) {
            Address direccion = new Address();
            direccion.setUse(Address.AddressUse.WORK);
                  
            if (direccionNode.has("descripcion")) {
                direccion.addLine(HapiFhirUtils.readStringValueFromJsonNode("descripcion",direccionNode));
            }
                else HapiFhirUtils.addNotFoundIssue("prestador.direccion.descripcion", oo);

            if (direccionNode.has("pais")) {
                String vs ="https://hl7chile.cl/fhir/ig/clcore/ValueSet/CodPais";
                String cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CodPais";
                String code = HapiFhirUtils.readStringValueFromJsonNode("codigo", direccionNode.get("pais"));
                 String validoS = validator.validateCode(cs,code,"",vs);

                if (validoS != null){
                    Coding coding = new Coding(cs,code,validoS);
                    CodeableConcept cc = new CodeableConcept(coding);
                    direccion.getCountryElement().addExtension(HapiFhirUtils.buildExtension(
                            "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/CodigoPaises",cc));
                } else HapiFhirUtils.addNotFoundCodeIssue("Prestador.direccion.pais",oo);
            }

            if (direccionNode.has("region")) {

                String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", direccionNode.get("region"));
                String vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosRegionesCL";
                String cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodRegionCL";
                String validoS = validator.validateCode(cs, codigo, "", vs);
                if (validoS != null) {
                    Coding coding = new Coding(cs, codigo, validoS);
                    CodeableConcept cc = new CodeableConcept(coding);
                    direccion.getStateElement().addExtension(HapiFhirUtils.buildExtension(
                            "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/RegionesCl", cc));
                }else {
                    HapiFhirUtils.addNotFoundCodeIssue("prestador.direccion.region.codigo", oo);
                }
            }

            if (direccionNode.has("provincia")) {

                String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", direccionNode.get("provincia"));
                String vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosProvinciasCL";
                String cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodProvinciasCL";
                String validoS = validator.validateCode(cs, codigo, "", vs);
                if (validoS != null) {
                    Coding coding = new Coding(cs, codigo, validoS);
                    CodeableConcept cc = new CodeableConcept(coding);
                    direccion.getDistrictElement().addExtension(HapiFhirUtils.buildExtension(
                            "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/ProvinciasCl", cc));
                } else {
                    HapiFhirUtils.addNotFoundCodeIssue("prestador.direccion.provincia.codigo", oo);
                }
            }


            if (direccionNode.has("comuna")) {

                JsonNode comunaJ = direccionNode.get("comuna");
                String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo",comunaJ);
                String vs = "https://hl7chile.cl/fhir/ig/clcore/ValueSet/VSCodigosComunaCL";
                String cs = "https://hl7chile.cl/fhir/ig/clcore/CodeSystem/CSCodComunasCL";

                String validoS = validator.validateCode(cs,codigo,"",vs);
                if (validoS != null){
                    Coding coding = new Coding(cs,codigo,validoS);
                    CodeableConcept cc = new CodeableConcept(coding);
                    direccion.getCityElement().addExtension(HapiFhirUtils.buildExtension(
                            "https://hl7chile.cl/fhir/ig/clcore/StructureDefinition/ComunasCl",
                            cc));
                } else HapiFhirUtils.addNotFoundCodeIssue("prestador.direccion.comuna.codigo", oo);
            }
            practitioner.addAddress(direccion);
        }


        if (node.has("titulosProfesionales")) {

            JsonNode tits = node.get("titulosProfesionales");
            boolean validoB = HapiFhirUtils.validateArrayInJsonNode("titulosProfesionales", tits,oo,true);
            if (validoB) {
                String vs = "";
                String cs = "";
                if(tipoPractitioner.equals("profesional")){
                    vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSTituloProfesional";
                    cs= "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSTituloProfesional";
                    addQualifications(practitioner, tits, cs, vs,"cert",oo, true);

                }
                if (tipoPractitioner.equals("administrativo")){
                    vs = "";
                    cs = "";
                    if(tits.size() >0) {
                        String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo", tits.get(0));
                        if (codigo == null)
                            HapiFhirUtils.addNotFoundIssue("prestador.titulosProfesionales.codigo", oo);
                        //String nombre = HapiFhirUtils.readStringValueFromJsonNode("nombre", tits.get(0));
                        addQualifications(practitioner, tits, cs, vs,"cert",oo, false);
                    }
                }
            }
            if (tipoPractitioner.equals("profesional")) {
                // Calificaciones (títulos, especialidades, subespecialidades, etc.)
                if(node.has("especialidadesMedicas")) {
                    HapiFhirUtils.validateArrayInJsonNode("especialidadesMedicas", node.get("especialidadesMedicas"), oo,true);
                    addQualifications(practitioner, node.get("especialidadesMedicas"),
                            "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed",
                            "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEspecialidadMed",
                            "esp", oo,true);
                }
                if(node.has("subespecialidadesMedicas")) {
                    HapiFhirUtils.validateArrayInJsonNode("subespecialidadesMedicas", node.get("subespecialidadesMedicas"), oo,false);
                    addQualifications(practitioner, node.get("subespecialidadesMedicas"),
                            "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed",
                            "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEspecialidadMed",
                            "subesp", oo,true);
                }
                if(node.has("especialidadesOdontologicas")) {
                    HapiFhirUtils.validateArrayInJsonNode("especialidadesOdontologicas", node.get("especialidadesOdontologicas"), oo,false);
                    addQualifications(practitioner, node.get("especialidadesOdontologicas"),
                            "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadOdont",
                            "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEspecialidadOdont",
                            "EspOdo", oo,true);
                }
                if(node.has("especialidadesBioquimicas")) {
                    HapiFhirUtils.validateArrayInJsonNode("especialidadesBioquimicas", node.get("especialidadesBioquimicas"), oo,false);
                    addQualifications(practitioner, node.get("especialidadesBioquimicas"),
                            "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadBioqca",
                            "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEspecialidadBioqca",
                            "EspBioQ", oo,true);
                }

                if(node.has("especialidadesFarmacologicas")) {
                    HapiFhirUtils.validateArrayInJsonNode("especialidadesFarmacologicas", node.get("especialidadesFarmacologicas"), oo,false);
                    addQualifications(practitioner, node.get("especialidadesFarmacologicas"),
                            "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadFarma",
                            "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEspecialidadFarma",
                            "EspFarma", oo,true);
                }
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

    private void addQualifications(Practitioner p, JsonNode node, String system, String vs, String identifierValue, OperationOutcome oo, boolean validateCode) {
        if (node != null && node.isArray()) {
            int i = 0;
            for (JsonNode q : node) {
                Practitioner.PractitionerQualificationComponent qual = new Practitioner.PractitionerQualificationComponent();
                qual.addIdentifier().setValue(identifierValue);
                String codigo = HapiFhirUtils.readStringValueFromJsonNode("codigo",q);
                String nombre = HapiFhirUtils.readStringValueFromJsonNode("nombre", q);
                String valido = "";

                if(validateCode) {
                    valido = validator.validateCode(system, codigo, nombre, vs);
                }
                if(valido != null || !validateCode) {
                    if(nombre != null){
                        valido = nombre;
                    }
                    qual.setCode(new CodeableConcept().addCoding(
                            new Coding()
                                    .setSystem(system)
                                    .setCode(codigo)
                                    .setDisplay(valido)
                    ).setText(valido));
                    // Periodo
                    try {
                        Date start = HapiFhirUtils.readDateValueFromJsonNode("fechaEmision", q);

                        if (start != null) {
                            Period period = new Period();
                            period.setStartElement(new DateTimeType(start));
                            qual.setPeriod(period);
                        }
                    }catch (ParseException e){
                        HapiFhirUtils.addErrorIssue("Prestador.Titulosprofesionales.fechaEmision",
                                "fechaEmision No valida",oo);
                        throw new RuntimeException(e);
                    }

                    String issuer = HapiFhirUtils.readStringValueFromJsonNode("institucion", q);
                    if (issuer != null) {
                        qual.setIssuer(new Reference().setDisplay(issuer));
                    }

                    String mencion = HapiFhirUtils.readStringValueFromJsonNode("mencion", q);
                    if (mencion != null){
                        qual.addExtension(new Extension(
                                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/Mencion",
                                new StringType(mencion)));
                    }

                    p.addQualification(qual);
                } else HapiFhirUtils.addNotFoundCodeIssue("prestador.titulosProfesionales["+i+"].codigo",oo);
                i++;
            }
        }
    }
}

