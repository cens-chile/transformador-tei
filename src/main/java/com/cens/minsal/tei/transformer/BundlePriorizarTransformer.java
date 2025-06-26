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
        boolean validate = HapiFhirUtils.validateObjectInJsonNode("datosSistema", get, out);
        if(validate){
            ((ObjectNode)get).put("tipoEvento", "priorizar");
            messageHeader = messageHeaderTransformer.transform(get, out);
        }

        get = node.get("solicitudIC");
        validate = HapiFhirUtils.validateObjectInJsonNode("solicitudIC", get, out);

        ServiceRequest sr = null;
        if(validate)
            sr = buildServiceRequest(node, out);

        get = node.get("prestadorProfesional");
        validate = HapiFhirUtils.validateObjectInJsonNode("prestadorProfesional", get, out);

        String tipoPrestador = "profesional";
        Practitioner practitioner = null;
        if(validate){
            practitioner = practitionerTransformer.transform(tipoPrestador,get, out);
        }



        /*get = node.get("paciente");
        Patient patient = null;
        if(get != null){
            ((ObjectNode)get).put("tipoEvento", "priorizar");
            patient = patientTransformer.transform(get, out);
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos del paciente", out);
        }
*/
        get = node.get("establecimiento");
        Organization organization = null;
        if(get != null){
            organization = organizationTransformer.transform(get, out,"");
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la organización", out);
        }




        //Rol del Profesional (practitionerRol)

        PractitionerRole priorizador = null;
        priorizador = practitionerRoleTransformer.buildPractitionerRole("priorizador",organization, practitioner);

        //Coding roleCode = new Coding("https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSPractitionerTipoRolLE", "terminador", "Terminador");
        //CodeableConcept cc = new CodeableConcept(roleCode);
        //priorizador.addCode(cc);

        priorizador.setPractitioner(new Reference(practitioner));
        priorizador.setOrganization(new Reference(organization));

        //Agrega recursos con sus respectivos UUID al bundle de respuesta

        HapiFhirUtils.addResourceToBundle(b,messageHeader);
        HapiFhirUtils.addResourceToBundle(b,priorizador);
        String referenciaPaciente = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente",node);
        if (referenciaPaciente == null) HapiFhirUtils.addNotFoundIssue("referenciaPaciente", out);
        sr.setSubject(new Reference(referenciaPaciente));
        HapiFhirUtils.addResourceToBundle(b,sr);
        HapiFhirUtils.addResourceToBundle(b,practitioner);
        //HapiFhirUtils.addResourceToBundle(b,patient);
        HapiFhirUtils.addResourceToBundle(b,organization);

        setMessageHeaderReferences(messageHeader, new Reference(sr), new Reference(priorizador));

        if (!out.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(out,fhirServerConfig.getFhirContext());
            return res;
        }
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

        String codigoEstadoIC = "4";

        if(codigoEstadoIC != null) {
            String csEIC = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEstadoInterconsulta";
            String vsEIC = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEstadoInterconsulta";
            String resValidacion = validator.validateCode(csEIC, codigoEstadoIC, "", vsEIC);
            if (resValidacion == null) {
                HapiFhirUtils.addErrorIssue(codigoEstadoIC, "solicitudIC.codigoEstadoIC No válido", oo);
            }
            CodeableConcept cc = new CodeableConcept();
            cc.addCoding(new Coding(
                    "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEstadoInterconsulta",
                    codigoEstadoIC,
                    resValidacion));

            Extension extensionEstadoIC = new Extension();
            extensionEstadoIC.setUrl("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEstadoInterconsultaCodigoLE");
            extensionEstadoIC.setValue(cc);
            sr.addExtension(extensionEstadoIC);
        } else {
            HapiFhirUtils.addNotFoundIssue("codigoEstadoIC", oo);
        }

        String prioridadIc = HapiFhirUtils.readStringValueFromJsonNode("prioridadIc", node);
        if(prioridadIc!=null){

            if(prioridadIc.equals("routine")){
                sr.setPriority(ServiceRequest.ServiceRequestPriority.ROUTINE);
            }
            else if(prioridadIc.equals("urgent")){
                sr.setPriority(ServiceRequest.ServiceRequestPriority.URGENT);
            }
            else
                HapiFhirUtils.addErrorIssue("prioridadIc","código no encontrado", oo);
        }
        else
            HapiFhirUtils.addNotFoundIssue("prioridadIc", oo);

        if(node.has("fundamentoPriorizacion")){
            String fundamento = HapiFhirUtils.readStringValueFromJsonNode("fundamentoPriorizacion",node);
            sr.addExtension(new Extension(
                    "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionStringFundamentoPriorizacion",
                    new StringType(fundamento)));
        }
        if(node.has("especialidadMedicaDestinoCodigo")){
            Extension espMedExt = new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEspecialidadMedicaDestinoCodigo");
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VsEspecialidadDest";
            String value = HapiFhirUtils.readStringValueFromJsonNode("especialidadMedicaDestinoCodigo",node);
            String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed";
            String cs2 = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadOdont";
            String valido = validator.validateCode(cs,value,"",vs);
            if(valido != null) {
                Coding cod = new Coding(vs, value, valido);
                CodeableConcept cc = new CodeableConcept(cod);
                espMedExt.setValue(cc);
            }
            valido = validator.validateCode(cs2,value,"",vs);
            if(valido != null) {
                Coding cod = new Coding(vs, value, valido);
                CodeableConcept cc = new CodeableConcept(cod);
                espMedExt.setValue(cc);
            }
            sr.addExtension(espMedExt);
        }

        //Subespecialidad
        if(node.has("subEspecialidadMedicaDestinoCodigo")){
            Extension subEspMedExt = new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionSubEspecialidadMedicaDestinoCodigo");
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VsEspecialidadDest";
            String value = HapiFhirUtils.readStringValueFromJsonNode("especialidadMedicaDestinoCodigo",node);
            String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed";
            String cs2 = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadOdont";
            String valido = validator.validateCode(cs,value,"",vs);
            if(valido != null) {
                Coding cod = new Coding(vs, value, valido);
                CodeableConcept cc = new CodeableConcept(cod);

                subEspMedExt.setValue(cc);
            }
            valido = validator.validateCode(cs2,value,"",vs);
            if(valido != null) {
                Coding cod = new Coding(vs, value, valido);
                CodeableConcept cc = new CodeableConcept(cod);
                subEspMedExt.setValue(cc);
            }
            sr.addExtension(subEspMedExt);
        }

        String modalidadAtencion = HapiFhirUtils.readStringValueFromJsonNode("modalidadAtencion", node);
        if(modalidadAtencion != null) {
            Coding coding = VSModalidadAtencionEnum.fromCode(modalidadAtencion).getCoding();
            sr.getCategoryFirstRep().addCoding(coding);
        }
        return sr;
    }
}
