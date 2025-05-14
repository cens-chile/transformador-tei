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
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

/**
 *
 * @author Jose <jose.m.andrade@gmail.com>
 */
public class HapiFhirUtils {
    
    public static Bundle.BundleEntryComponent findBundleEntryComponentByResourceOnLocation(Bundle bundle,Class resourceType)
    {
       
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
        for (Bundle.BundleEntryComponent entry : entries){
            System.out.println("entry = " + entry.getResponse().getLocation());
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
    
    
    public static String readStringValueFromJsonNode(String value, JsonNode node){
        
        JsonNode get = node.get(value);
        if(get!=null && !get.asText().isBlank())
            return get.asText();
        return null;
    }
    
    public static Date readDateValueFromJsonNode(String value, JsonNode node) throws ParseException{
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        JsonNode get = node.get(value);
        if(get!=null && !get.asText().isBlank()){
            System.out.println("get = " + get.asText());
            return formatter.parse(get.asText());
        }
        return null;
    }
}
