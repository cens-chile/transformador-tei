/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.valuesets.VSModalidadAtencionEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class BundlePriorizarTransformer {

    FhirServerConfig fhirServerConfig;
    static final String bundleProfile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/BundlePriorizarLE";

    MessageHeaderTransformer messageHeaderTransformer;
    PractitionerTransformer practitionerTransformer;
    PractitionerRoleTransformer practitionerRoleTransformer;
    PatientTransformer patientTransformer;
    OrganizationTransformer organizationTransformer;
    ValueSetValidatorService validator;



    public BundlePriorizarTransformer(FhirServerConfig fhirServerConfig,
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
            node = mapper.readTree(cmd);

        } catch (JsonProcessingException ex) {
            Logger.getLogger(BundlePriorizarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }



        
        JsonNode get = node.get("datosSistema");
        MessageHeader messageHeader = null;
        MessageHeaderTransformer messageHeaderTransformer = new MessageHeaderTransformer(validator);
        ((ObjectNode)get).put("tipoEvento", "priorizar");


        if(get!=null)
            messageHeader = messageHeaderTransformer.transform(get, out);
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

        //Rol del Profesional (practitionerRol)

        PractitionerRole practitionerRole = null;
        practitionerRole = practitionerRoleTransformer.transform(get, out);
        Coding roleCode = new Coding("https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSPractitionerTipoRolLE", "terminador", "Terminador");
        CodeableConcept cc = new CodeableConcept(roleCode);
        practitionerRole.addCode(cc);

        practitionerRole.setPractitioner(new Reference("Practitioner/"+practitioner.getId().toString()));
        practitionerRole.setOrganization(new Reference("Organization/"+organization.getIdentifier().get(0).getValue().toString()));

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

        IdType pracRolId = IdType.newRandomUuid();
        b.addEntry().setFullUrl(pracRolId.getIdPart())
                .setResource(practitionerRole);

/*
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

        Reference pacienteRef = new Reference(HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente",nodeOrigin));
        if(pacienteRef != null ){
            sr.setSubject(pacienteRef);
        }else HapiFhirUtils.addNotFoundIssue("referenciaPaciente",oo);

        JsonNode node = nodeOrigin.get("solicitudIC");
        try {
            Date d = HapiFhirUtils.readDateValueFromJsonNode("fechaSolicitudIC", node);
            sr.setAuthoredOn(d);
        } catch (ParseException ex) {
            Logger.getLogger(BundlePriorizarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            HapiFhirUtils.addErrorIssue("fechaSolicitudIC", ex.getMessage(), oo);
        }

        String idIC = HapiFhirUtils.readStringValueFromJsonNode("idInterconsulta", node);
        if (idIC == null) HapiFhirUtils.addNotFoundIssue("idInterconsulta", oo);
        Identifier identifierIC = new Identifier().setValue(idIC);
        sr.addIdentifier(identifierIC);

        //codigoEstadoIC

        String codigoEstadoIC = HapiFhirUtils.readStringValueFromJsonNode("codigoEstadoIC", node);

        if(codigoEstadoIC != null){
            String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEstadoInterconsulta";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEstadoInterconsulta";
            String resValidacion = validator.validateCode(cs,codigoEstadoIC,"",vs);
            if (resValidacion == null){HapiFhirUtils.addErrorIssue(codigoEstadoIC, "No válido", oo ); }
        }


            sr.addExtension(HapiFhirUtils.buildExtension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEstadoInterconsultaCodigoLE",
                new StringType(codigoEstadoIC)));

        String modalidadAtencion = HapiFhirUtils.readStringValueFromJsonNode("modalidadAtencion", node);
        Coding coding = VSModalidadAtencionEnum.fromCode(modalidadAtencion).getCoding();
        sr.getCategoryFirstRep().addCoding(coding);

        String codigoMotivoCierreIC = HapiFhirUtils.readStringValueFromJsonNode("codigoMotivoCierreIC", node);
        if(codigoMotivoCierreIC == null) HapiFhirUtils.addNotFoundIssue("codigoMotivoCierreIC", oo);
        String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSMotivoCierreInterconsulta";
        String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSMotivoCierreInterconsulta";
        String resValidacion = validator.validateCode(cs,codigoMotivoCierreIC,"",vs);

        if (resValidacion == null){HapiFhirUtils.addErrorIssue(codigoMotivoCierreIC, "No válido", oo ); }

        String glosaCierreIC = HapiFhirUtils.readStringValueFromJsonNode("glosaCierreIC", node);
        if(glosaCierreIC == null) HapiFhirUtils.addNotFoundIssue("glosaCierreIC", oo);
        String sistemaMotivoCierreIC = HapiFhirUtils.readStringValueFromJsonNode("sistemaMotivoCierreIC", node);
        if(sistemaMotivoCierreIC == null) HapiFhirUtils.addNotFoundIssue("sistemaMotivoCierreIC", oo);
        CodeableConcept cc = new CodeableConcept();
        Coding codingCierreIC = new Coding(sistemaMotivoCierreIC,codigoMotivoCierreIC, glosaCierreIC);
        cc.addCoding(codingCierreIC);
        Type type = cc;
        sr.addExtension(HapiFhirUtils.buildExtension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionMotivoCierreInterconsulta",
        type));


        return sr;
    }
}
