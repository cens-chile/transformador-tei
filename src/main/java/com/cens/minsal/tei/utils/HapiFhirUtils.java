/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import com.fasterxml.jackson.databind.JsonNode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

/**
 *
 * @author Jose <jose.m.andrade@gmail.com>
 */
public class HapiFhirUtils {
    
    public static final String snomdeSystem = "http://snomed.info/sct";
    public static final String loincSystem = "http://loinc.org";
    
    public static Bundle.BundleEntryComponent findBundleEntryComponentByResourceOnLocation(Bundle bundle,Class resourceType)
    {
       
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
        for (Bundle.BundleEntryComponent entry : entries){
            if(entry.hasResponse() && entry.getResponse().getLocation().contains(resourceType.getSimpleName())){
                entries.remove(entry);
                return entry;
            }
        }
        return null;
    }
    
    public static void printResource(Resource r){
        
        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        String encodeResourceToString = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(r);
        //String serialized = parser.encodeResourceToString(r);
        System.out.println(encodeResourceToString);
    }
    
    public static String resourceToString(Resource r,FhirContext ctx){

        IParser parser = ctx.newJsonParser();
        String encodeResourceToString = ctx.newJsonParser().setPrettyPrint(false).encodeResourceToString(r);
        return encodeResourceToString;
    }
    
    public static <T extends IBaseResource> T stringToResource(String text, Class<T> theClass) {
        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        return (T) parser.parseResource(theClass, text);
    }
    
    public static <T extends IBaseResource> T findResourceByClass(Bundle bundle, Class<T> theClass) {

        for(var e: bundle.getEntry()){
            if(e.getResource().getClass() == theClass)
                return (T) e.getResource();
        }
        return null;
    }
    
    public static void addNotFoundIssue(String value, OperationOutcome out){
        OperationOutcome.OperationOutcomeIssueComponent issue;
        issue = new OperationOutcome.OperationOutcomeIssueComponent();
        issue.setCode(OperationOutcome.IssueType.NOTFOUND);
        issue.setDiagnostics(value+" not found or not have a value");
        out.getIssue().add(issue);
    }
    
    public static void addInvalidIssue(String value, OperationOutcome out){
        OperationOutcome.OperationOutcomeIssueComponent issue;
        issue = new OperationOutcome.OperationOutcomeIssueComponent();
        issue.setCode(OperationOutcome.IssueType.INVALID);
        issue.setDiagnostics(value+" is invalid");
        out.getIssue().add(issue);
    }
    
    public static void addErrorIssue(String value, String message, OperationOutcome out){
        OperationOutcome.OperationOutcomeIssueComponent issue;
        issue = new OperationOutcome.OperationOutcomeIssueComponent();
        issue.setCode(OperationOutcome.IssueType.EXCEPTION);
        issue.setDiagnostics(value+" have errors in definition ["+message+"]");
        out.getIssue().add(issue);
    }

    public static String transformarFecha(String fechaOriginal) throws ParseException {
        if (fechaOriginal == null || fechaOriginal.isBlank()) {
            return fechaOriginal;
        }

        // Validar si ya está en formato yyyy-MM-dd
        if (fechaOriginal.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return fechaOriginal;
        }

        // Intentar transformar desde dd-MM-yyyy a yyyy-MM-dd
        SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd-MM-yyyy");
        formatoEntrada.setLenient(false);

        SimpleDateFormat formatoSalida = new SimpleDateFormat("yyyy-MM-dd");

        Date fecha = formatoEntrada.parse(fechaOriginal); // lanza ParseException si es inválido
        return formatoSalida.format(fecha);

    }


    
    public static String readIntValueFromJsonNode(String value, JsonNode node){
        JsonNode valueNode = node.get(value);
        if (valueNode != null) {
            if (valueNode.isInt() || valueNode.isLong()) {
                return valueNode.asText();
            } else if (valueNode.isTextual()) {
                String text = valueNode.asText();
                try {
                    // Intentamos parsear el texto como un número
                    Integer.parseInt(text); // O usar Long.parseLong(text) si esperas valores grandes
                    return text;
                } catch (NumberFormatException e) {
                    // No es un número válido, devolver null
                    return null;
                }
            }
        }
        return null;
    }


    public static String readStringValueFromJsonNode(String value, JsonNode node){
        
        JsonNode get = node.get(value);
        if(get!=null && !get.asText().isBlank() && get.isTextual())
            return get.asText();
        return null;
    }



    public static Date readDateValueFromJsonNode(String value, JsonNode node) throws ParseException {
        JsonNode get = node.get(value);
        if (get != null && !get.asText().isBlank()) {
            String dateText = get.asText();
            ParseException lastException = null;

            for (String pattern : new String[]{"dd-MM-yyyy HH:mm:ss", "dd-MM-yyyy"}) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat(pattern);
                    //formatter.setLenient(false); // Para evitar que Java acepte fechas inválidas
                    return formatter.parse(dateText);
                } catch (ParseException e) {
                    lastException = e;
                }
            }

            // Si no se pudo parsear con ninguno de los formatos
            throw lastException;
        }

        return null;
    }


    public static Extension buildBooleanExt(String url,boolean value){
        Extension ex =  new Extension(url);
        ex.setValue(new BooleanType(value));
        return ex;
    }

    public static Extension buildExtension(String url, Type t){
        Extension ex =  new Extension(url);
        ex.setValue(t);
        return ex;
    }

    public static Boolean readBooleanValueFromJsonNode(String fieldName, JsonNode node) {
        JsonNode field = node.get(fieldName);
        if (field != null && field.isBoolean()) {
            return field.booleanValue();
        } else if (field != null && field.isTextual()) {
            return Boolean.parseBoolean(field.textValue());
        }
        return null;
    }

    /**
     * Agrega una extensión a un campo de tipo String en Address, con un CodeableConcept como valor.
     *
     * @param address El objeto Address al cual se le quiere agregar la extensión.
     * @param fieldName El nombre del campo (por ejemplo: "city", "district", "state", "country").
     * @param extensionUrl La URL de la estructura de la extensión (ej: RegionesCl, ComunasCl, etc.).
     * @param system El sistema de codificación (ej: CodeSystem oficial).
     * @param code El código del valor a agregar.
     * @param display El valor textual que representa al código.
     */
    public static void addCodigoExtension(Address address, String fieldName, String extensionUrl,
                                          String system, String code, String display) {
        Extension extension = new Extension();
        extension.setUrl(extensionUrl);

        CodeableConcept concept = new CodeableConcept();
        concept.addCoding(new Coding(system, code, display));
        extension.setValue(concept);

        switch (fieldName) {
            case "ciudad":
                address.getCityElement().addExtension(extension);
                break;
            case "provincia":
                address.getDistrictElement().addExtension(extension);
                break;
            case "region":
                address.getStateElement().addExtension(extension);
                break;
            case "pais":
                address.getCountryElement().addExtension(extension);
                break;
            default:
                throw new IllegalArgumentException("Campo de dirección no soportado para extensión: " + fieldName);
        }
    }

    public static CodeableConcept buildCodeableConceptFromJson(JsonNode node) {
        if (node == null || node.isEmpty()) return null;

        CodeableConcept concept = new CodeableConcept();
        JsonNode codingNode = node.get("coding");
        if (codingNode != null && codingNode.isArray()) {
            for (JsonNode cod : codingNode) {
                String system = cod.has("system") ? cod.get("system").asText() : null;
                String code = cod.has("code") ? cod.get("code").asText() : null;
                String display = cod.has("display") ? cod.get("display").asText() : null;

                if (system != null && code != null) {
                    concept.addCoding(new Coding(system, code, display));
                }
            }
        }

        if (node.has("text")) {
            concept.setText(node.get("text").asText());
        }

        return concept;
    }


    public static Date readDateTimeValueFromJsonNode(String field, JsonNode node, String format) throws ParseException {
        JsonNode value = node.get(field);
        if (value != null && !value.asText().isBlank()) {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.parse(value.asText());
        }
        return null;
    }

    public static void addCodigoExtension(StringType cityElement, String extensionUrl, String system, JsonNode direccion, String s) {
    }
}
