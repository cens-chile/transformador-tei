/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.utils.JsonUniqueKeyValidator;
import com.cens.minsal.tei.valuesets.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.cens.minsal.tei.services.ValueSetValidatorService;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Juan F. <jfanasco@cens.cl>
 */
@Component
public class BundleTerminarTransformer {

    FhirServerConfig fhirServerConfig;
    static final String bundleProfile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/BundleTerminarLE";

    MessageHeaderTransformer messageHeaderTransformer;
    PractitionerTransformer practitionerTransformer;
    PractitionerRoleTransformer practitionerRoleTransformer;
    PatientTransformer patientTransformer;
    OrganizationTransformer organizationTransformer;
    ValueSetValidatorService validator;



    public BundleTerminarTransformer(FhirServerConfig fhirServerConfig,
                                     MessageHeaderTransformer messageHeaderTransformer,
                                     PractitionerTransformer practitionerTransformer,
                                     PatientTransformer patientTransformer,
                                     PractitionerRoleTransformer practitionerRoleTransformer,
                                     OrganizationTransformer organizationTransformer,
                                     ValueSetValidatorService validator) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
        this.practitionerTransformer = practitionerTransformer;
        this.patientTransformer = patientTransformer;
        this.organizationTransformer = organizationTransformer;
        this.practitionerRoleTransformer = practitionerRoleTransformer;
        this.validator = validator;
    }
    
    
    public String buildBundle(String cmd) {
        ObjectMapper mapper = new ObjectMapper();

        String res;
        OperationOutcome out = new OperationOutcome();
        
        Bundle b = new Bundle();
        b.getMeta().addProfile(bundleProfile);
        b.setType(Bundle.BundleType.MESSAGE);
        b.setTimestamp(new Date());

        JsonNode node;
        try {
            JsonUniqueKeyValidator.validateUniqueKeys(cmd, out);
            node = mapper.readTree(cmd);

        } catch (JsonProcessingException ex) {
            Logger.getLogger(BundleTerminarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }



        
        JsonNode get = node.get("datosSistema");
        MessageHeader messageHeader = null;
        boolean datosSistemasVal = HapiFhirUtils.validateObjectInJsonNode("datosSistema", get, out,true);
        if(datosSistemasVal) {
            MessageHeaderTransformer messageHeaderTransformer = new MessageHeaderTransformer(validator);
            ((ObjectNode) get).put("tipoEvento", "terminar");
            messageHeader = messageHeaderTransformer.transform(get, out);
           }else
            HapiFhirUtils.addNotFoundIssue("datosSistema", out);


        get = node.get("solicitudIC");
        ServiceRequest sr = null;
        boolean srVal = HapiFhirUtils.validateObjectInJsonNode("solicitudIC", get,out,true);
        if(srVal) {
            sr = buildServiceRequest(get, out);
        }else
            HapiFhirUtils.addNotFoundIssue("solicitudIC", out);

        get = node.get("prestador");
        boolean prestadorValid = HapiFhirUtils.validateObjectInJsonNode("prestador", get,out,true);
        Practitioner practitioner = null;
        if(prestadorValid) {
            String tipoPrestador = HapiFhirUtils.readStringValueFromJsonNode("tipoPrestador", get);
            if (!tipoPrestador.toLowerCase().equals("profesional") && !tipoPrestador.toLowerCase().equals("administrativo")) {
                HapiFhirUtils.addErrorIssue("prestador.tipoPrestador", "Dato no válido", out);
            }
            practitioner = practitionerTransformer.transform(tipoPrestador, get, out);
        }
        else{
            HapiFhirUtils.addNotFoundIssue("Prestador", out);
        }

        get = node.get("establecimiento");
        boolean estabValid = HapiFhirUtils.validateObjectInJsonNode("establecimiento", get,out,true);

        Organization organization = null;
        if(estabValid){
            organization = organizationTransformer.transform(get, out,"");
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la organización", out);
        }
        PractitionerRole terminador = null;
        if(organization != null && practitioner != null) {
            terminador = practitionerRoleTransformer.buildPractitionerRole("terminador", organization, practitioner);
        }

        if (!out.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(out,fhirServerConfig.getFhirContext());
            return res;
        }

        HapiFhirUtils.addResourceToBundle(b, messageHeader);
        setMessageHeaderReferences(messageHeader, new Reference(sr), new Reference(terminador));

        //Rol del Profesional (practitionerRol)
        String srFullUrl = HapiFhirUtils.getUrlBaseFullUrl()+"/ServiceRequest/"+sr.getId();
        HapiFhirUtils.addResourceToBundle(b, sr,srFullUrl);

        String refPat = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente", node);
        sr.getSubject().setReference(refPat);

        sr.getPerformer().add(new Reference(terminador));

        HapiFhirUtils.addResourceToBundle(b,practitioner);
        HapiFhirUtils.addResourceToBundle(b,organization);
        HapiFhirUtils.addResourceToBundle(b,terminador);

        res = HapiFhirUtils.resourceToString(b, fhirServerConfig.getFhirContext());
        return res;
    }

    public void setMessageHeaderReferences(MessageHeader m, Reference sr, Reference pr){
        m.setAuthor(pr);
        m.getFocus().add(sr);
    }
    
    
    public ServiceRequest buildServiceRequest(JsonNode nodeOrigin, OperationOutcome oo){
        String profile ="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ServiceRequestLE";
        ServiceRequest sr = new ServiceRequest();
        sr.getMeta().addProfile(profile);
        sr.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

        String id = HapiFhirUtils.readStringValueFromJsonNode("idSolicitudServicio", nodeOrigin);

        if(id!=null)
            sr.setId(id);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC.idSolicitudServicio", oo);

        try {
            Date d = HapiFhirUtils.readDateValueFromJsonNode("fechaSolicitudIC", nodeOrigin);
            sr.setAuthoredOn(d);
        } catch (ParseException ex) {
            Logger.getLogger(BundleTerminarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            HapiFhirUtils.addErrorIssue("fechaSolicitudIC", ex.getMessage(), oo);
        }

        String idIC = HapiFhirUtils.readStringValueFromJsonNode("idInterconsulta", nodeOrigin);
        if (idIC == null) HapiFhirUtils.addNotFoundIssue("idInterconsulta", oo);
        Identifier identifierIC = new Identifier().setValue(idIC);
        sr.addIdentifier(identifierIC);

        //codigoEstadoIC

        String codigoEstadoIC = "7";

        if(codigoEstadoIC != null){
            String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEstadoInterconsulta";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEstadoInterconsulta";
            String resValidacion = validator.validateCode(cs,codigoEstadoIC,"",vs);
            if (resValidacion == null){HapiFhirUtils.addErrorIssue(codigoEstadoIC, "No válido", oo ); }
            Coding coding = new Coding(cs,codigoEstadoIC,resValidacion);
            CodeableConcept cc = new CodeableConcept(coding);
            sr.addExtension(HapiFhirUtils.buildExtension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEstadoInterconsultaCodigoLE",
                    cc));
        }


        String modalidadAtencion = HapiFhirUtils.readStringValueFromJsonNode("modalidadAtencion", nodeOrigin);
        Coding coding = VSModalidadAtencionEnum.fromCode(modalidadAtencion).getCoding();
        sr.getCategoryFirstRep().addCoding(coding);

        String codigoMotivoCierreIC = HapiFhirUtils.readStringValueFromJsonNode("codigoMotivoCierreIC", nodeOrigin);
        if(codigoMotivoCierreIC == null) HapiFhirUtils.addNotFoundIssue("solicitudIC.codigoMotivoCierreIC", oo);
        String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSMotivoCierreInterconsulta";
        String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSMotivoCierreInterconsulta";
        String resValidacion = validator.validateCode(cs,codigoMotivoCierreIC,"",vs);

        if (resValidacion == null) HapiFhirUtils.addErrorIssue(codigoMotivoCierreIC, "No válido", oo );


        CodeableConcept cc = new CodeableConcept();
        Coding codingCierreIC = new Coding(cs,codigoMotivoCierreIC, resValidacion);
        cc.addCoding(codingCierreIC);
        sr.addExtension(HapiFhirUtils.buildExtension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionMotivoCierreInterconsulta",
        cc));

        return sr;
    }
}
