/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.jena.base.Sys;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;
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


    public BundleTerminarTransformer(FhirServerConfig fhirServerConfig,
                                     MessageHeaderTransformer messageHeaderTransformer,
                                     PractitionerTransformer practitionerTransformer,
                                     PatientTransformer patientTransformer,
                                     OrganizationTransformer organizationTransformer) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
        this.practitionerTransformer = practitionerTransformer;
        this.patientTransformer = patientTransformer;
        this.organizationTransformer = organizationTransformer;
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
            node = mapper.readTree(cmd);

        } catch (JsonProcessingException ex) {
            Logger.getLogger(BundleTerminarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }



        
        JsonNode get = node.get("datosSistema");
        MessageHeader messageHeader = null;
        ((ObjectNode)get).put("tipoEvento", "terminar");

        if(get!=null)
            messageHeader = 
                messageHeaderTransformer.transform(get, out);
        else
            HapiFhirUtils.addNotFoundIssue("datosSistema", out);


        get = node.get("solicitudIC");
        ServiceRequest sr = null;


        if(get!=null)
            sr = buildServiceRequest(node, out);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC", out);


        get = node.get("prestador");
        String tipoPrestador = HapiFhirUtils.readStringValueFromJsonNode("tipoPrestador", get);
        if(!tipoPrestador.toLowerCase().equals("profesional") && !tipoPrestador.toLowerCase().equals("administrativo")){
            HapiFhirUtils.addErrorIssue("Tipo Prestador", "Dato no válido", out);
        }
        Practitioner practitioner = null;

        if(get!=null && tipoPrestador != null){
            practitioner = practitionerTransformer.transform(tipoPrestador,get, out);
        }
        else{
            HapiFhirUtils.addNotFoundIssue("Prestador", out);
        }

        // Rol del Profesional (practitionerRol)
        /*
        get = node.get("rolDelProfesional");
        PractitionerRole practitionerRole = null;
        if(get != null){
            practitionerRole = practitionerRoleTransformer.transform(get, out);
            Coding roleCode = new Coding("https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSPractitionerTipoRolLE", "terminador", "Terminador");
            CodeableConcept cc = new CodeableConcept(roleCode);
            practitionerRole.addCode(cc);
        } else {
            HapiFhirUtils.addNotFoundIssue("Rol de profesional no definido", out);
        }
        */

        /*
        get = node.get("paciente");
        Patient patient = null;
        if(get != null){
            patient = patientTransformer.transform(get, out);
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos del paciente", out);
        }*/

        get = node.get("establecimiento");
        Organization organization = null;
        if(get != null){
            organization = organizationTransformer.transform(get, out,"");
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la organización", out);
        }


        if (!out.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(out,fhirServerConfig.getFhirContext());
            return res;
        }


        //Agrega recursos con sus respectivos UUID al bundle de respuesta
        IdType mHId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(mHId.getIdPart())
                .setResource(messageHeader);

        IdType sRId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(sRId.getIdPart())
                .setResource(sr);

        IdType pAId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pAId.getIdPart())
                .setResource(practitioner);
/*
        IdType pracRolId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pracRolId.getIdPart())
                .setResource(practitionerRole);


        IdType patId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(patId.getIdPart())
                .setResource(patient);
*/
        IdType orgId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(orgId.getIdPart())
                .setResource(organization);

        setMessageHeaderReferences(messageHeader, new Reference(sRId.getValue()), new Reference(pAId.getValue()));

        
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

        JsonNode pacienteJ =  nodeOrigin.get("paciente");
        String idPaciente = HapiFhirUtils.readStringValueFromJsonNode("id", pacienteJ);
        if(pacienteJ != null && idPaciente != null){
            sr.setSubject(new Reference("Patient/"+idPaciente));
        }else HapiFhirUtils.addNotFoundIssue("SolicitudIC.Paciente.id",oo);

        JsonNode node = nodeOrigin.get("solicitudIC");
        try {
            Date d = HapiFhirUtils.readDateValueFromJsonNode("fechaSolicitudIC", node);
            sr.setAuthoredOn(d);
        } catch (ParseException ex) {
            Logger.getLogger(BundleTerminarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            HapiFhirUtils.addErrorIssue("fechaSolicitudIC", ex.getMessage(), oo);
        }

        String idIC = HapiFhirUtils.readStringValueFromJsonNode("idInterconsulta", node);
        Identifier identifierIC = new Identifier().setValue(idIC);
        sr.addIdentifier(identifierIC);

        //codigoEstadoIC

        String codigoEstadoIC = HapiFhirUtils.readStringValueFromJsonNode("codigoEstadoIC", node);
        sr.addExtension(HapiFhirUtils.buildExtension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEstadoInterconsultaCodigoLE",
                new StringType(codigoEstadoIC)));

        String modalidadAtencion = HapiFhirUtils.readStringValueFromJsonNode("modalidadAtencion", node);
        Coding coding = VSModalidadAtencionEnum.fromCode(modalidadAtencion).getCoding();
        sr.getCategoryFirstRep().addCoding(coding);

        String codigoMotivoCierreIC = HapiFhirUtils.readStringValueFromJsonNode("codigoMotivoCierreIC", node);
        String glosaCierreIC = HapiFhirUtils.readStringValueFromJsonNode("glosaCierreIC", node);
        String sistemaMotivoCierreIC = HapiFhirUtils.readStringValueFromJsonNode("sistemaMotivoCierreIC", node);
        CodeableConcept cc = new CodeableConcept();
        Coding codingCierreIC = new Coding(sistemaMotivoCierreIC,codigoMotivoCierreIC, glosaCierreIC);
        cc.addCoding(codingCierreIC);
        Type type = cc;
        sr.addExtension(HapiFhirUtils.buildExtension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionMotivoCierreInterconsulta",
        type));


        return sr;
    }
}
