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
import jakarta.json.Json;
import org.hl7.fhir.dstu3.model.codesystems.CarePlanActivityCategory;
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
public class BundleAtenderTransformer {

    FhirServerConfig fhirServerConfig;
    static final String bundleProfile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/BundleAtenderLE";

    MessageHeaderTransformer messageHeaderTransformer;
    PractitionerTransformer practitionerTransformer;
    PractitionerRoleTransformer practitionerRoleTransformer;
    PatientTransformer patientTransformer;
    OrganizationTransformer organizationTransformer;
    CarePlanTransformer carePlanTransformer;
    EncounterTransformer encounterTransformer;
    ValueSetValidatorService validator;
    ServiceRequestTransformer serTransformer;


    public BundleAtenderTransformer(FhirServerConfig fhirServerConfig,
                                    MessageHeaderTransformer messageHeaderTransformer,
                                    PractitionerTransformer practitionerTransformer,
                                    PatientTransformer patientTransformer,
                                    OrganizationTransformer organizationTransformer,
                                    CarePlanTransformer carePlanTransformer,
                                    PractitionerRoleTransformer practitionerRoleTransformer,
                                    EncounterTransformer encounterTransformer,
                                    ServiceRequestTransformer serTransformer,
                                    ValueSetValidatorService validator) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
        this.practitionerTransformer = practitionerTransformer;
        this.patientTransformer = patientTransformer;
        this.organizationTransformer = organizationTransformer;
        this.carePlanTransformer = carePlanTransformer;
        this.encounterTransformer = encounterTransformer;
        this.validator = validator;
        this.practitionerRoleTransformer = practitionerRoleTransformer;
        this.serTransformer = serTransformer;
    }
    
    
    public String buildBundle(String cmd) {
        ObjectMapper mapper = new ObjectMapper();
        OperationOutcome oo = new OperationOutcome();
        String res;


        Bundle b = new Bundle();
        b.getMeta().addProfile(bundleProfile);
        b.setType(Bundle.BundleType.MESSAGE);
        b.setTimestamp(new Date());

        JsonNode node;
        try {
            node = mapper.readTree(cmd);

        } catch (JsonProcessingException ex) {
            Logger.getLogger(BundleAtenderTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }

        JsonNode get = node.get("datosSistema");
        MessageHeader messageHeader = null;
        boolean datosSistemasVal = HapiFhirUtils.validateObjectInJsonNode("datosSistema", get, oo,true);
        if(datosSistemasVal) {
            ((ObjectNode) get).put("tipoEvento", "atender");
            messageHeader =
                    messageHeaderTransformer.transform(get, oo);
        }

        get = node.get("solicitudIC");
        ServiceRequest sr = null;
        String srFullUrl = null;
        boolean srVal = HapiFhirUtils.validateObjectInJsonNode("solicitudIC", get,oo,true);
        if(srVal) {
            sr = buildServiceRequest(get, oo);
            srFullUrl = HapiFhirUtils.getUrlBaseFullUrl()+"/ServiceRequest/"+sr.getId();

        }

        // Prestador
        get = node.get("prestador");
        Practitioner practitioner = null;
        boolean prestadorValid = HapiFhirUtils.validateObjectInJsonNode("prestador", get,oo,true);
        String tipoPrestador = null;
        if(prestadorValid) {
            tipoPrestador = HapiFhirUtils.readStringValueFromJsonNode("tipoPrestador", get);
            if (tipoPrestador != null) {
                if (!tipoPrestador.toLowerCase().equals("profesional") && !tipoPrestador.toLowerCase().equals("administrativo")) {
                    HapiFhirUtils.addErrorIssue("Prestador.tipoPrestador", "Dato no válido", oo);
                }
                practitioner = practitionerTransformer.transform(tipoPrestador, get, oo);
            } else HapiFhirUtils.addNotFoundIssue("prestador.tipoPrestador", oo);
        }

        get = node.get("establecimiento");
        boolean estableValid = HapiFhirUtils.validateObjectInJsonNode("establecimiento", get,oo,true);
        Organization organization = null;
        if(estableValid){
            organization = organizationTransformer.transform(get, oo,"");
        }


        String patient = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente", node);
        if(patient == null){
            HapiFhirUtils.addNotFoundIssue("referenciaPaciente",oo);
        }
        AllergyIntoleranceTransformer at = new AllergyIntoleranceTransformer(validator);

        List<AllergyIntolerance> alergias = new ArrayList();
        if(node.has("alergias")){
            JsonNode allergNode = node.get("alergias");
            boolean allergValid = HapiFhirUtils.validateArrayInJsonNode("alergoas", allergNode, oo, false);
            if(allergValid)
                alergias = at.transform(allergNode, oo);
         }

        for (AllergyIntolerance a: alergias){
            a.setPatient(new Reference(patient));
        }

        List<ServiceRequest> examenSolicitados= serTransformer.buildSolicitudExamen(node, oo);

        List<Observation> examenes = new ArrayList();
        JsonNode resultados = node.get("resultadoExamenes");
        boolean resultsValid = HapiFhirUtils.validateArrayInJsonNode("resultadoExamenes", resultados,oo,false);
        if(resultsValid){
            ObservationTransformer observationTransformer = new ObservationTransformer(validator);
            examenes = observationTransformer.buildResultadoExamen(resultados, oo);
        }

        PractitionerRole atendedor = null;
        if(organization != null && practitioner != null) {
            atendedor = practitionerRoleTransformer.buildPractitionerRole("atendedor", organization, practitioner);
            sr.getPerformer().add(new Reference(atendedor));
        }


        String refPat = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente", node);
        if(sr != null) {
            sr.getSubject().setReference(refPat);
        }
        Encounter encounter = null;
        if(node.has("encuentroAtender")) {
            JsonNode encuentroNode = node.get("encuentroAtender");
            boolean encuentroValid = HapiFhirUtils.validateObjectInJsonNode("encuentroAtender", encuentroNode, oo, true);
            if (encuentroValid) {
                encounter = encounterTransformer.transform(node, oo, "atender");
                if (patient != null)
                    encounter.setSubject(new Reference(patient));
                if (sr != null)
                    encounter.addBasedOn(new Reference(sr));

                if (organization != null)
                    encounter.setServiceProvider(new Reference(organization));
            }
        } else
            HapiFhirUtils.addNotFoundIssue("encuentroAtender", oo);
        ConditionTransformer conditionTransformer = new ConditionTransformer(validator);
        Condition cond = null;
        if(node.has("diagnostico")){
            JsonNode diagnostico = node.get("diagnostico");
            boolean diagValid = HapiFhirUtils.validateObjectInJsonNode("diagnostico", diagnostico,oo,true);
            if (diagValid) {
                cond = conditionTransformer.transform(node.get("diagnostico"), oo, "diagnostico");
                cond.setSubject(new Reference(patient));
                if(encounter != null) {
                    encounter.addDiagnosis(new Encounter.DiagnosisComponent(new Reference(cond)));
                }
            }else {
                HapiFhirUtils.addInvalidIssue("diagnostico", oo);
            }
        }else {
            HapiFhirUtils.addNotFoundIssue("diagnostico", oo);
        }



        ObservationAnamnesisTransformer anamnesisTransformer = new ObservationAnamnesisTransformer(validator);
        Observation anam = null;

        if(node.has("anamnesis")){
            JsonNode anamNode = node.get("anamnesis");
            boolean anamValid = HapiFhirUtils.validateObjectInJsonNode("anamnesis", anamNode, oo, false);
            if(anamValid){
                anam = anamnesisTransformer.transform(anamNode,oo);
                if(patient != null) {
                    anam.setSubject(new Reference(patient));
                }
                if(encounter != null) {
                    anam.setEncounter(new Reference(encounter));
                }
            }
        }

        // Plan de atencion -Careplan

        get = node.get("planDeAtencion");
        CarePlan  careplan = null;
        boolean planValid = HapiFhirUtils.validateObjectInJsonNode("planDeAtencion",get, oo, true);
        if(planValid){
            careplan = carePlanTransformer.transform(get, oo);

            if(patient != null){
                careplan.setSubject(new Reference(patient));
            }

            if(encounter != null) {
                careplan.setEncounter(new Reference(encounter));
            }

            String descripcionPlan = HapiFhirUtils.readStringValueFromJsonNode("descripcion",get);
            if(descripcionPlan == null){
                HapiFhirUtils.addNotFoundIssue("plandeAtencion.descripcion", oo);
            } else{
                careplan.setDescription(descripcionPlan);
            }
        }

        //  - recetas (MedicationRequest)

        MedicationRequestTransformer mrt = new MedicationRequestTransformer(validator);
        MedicationRequest medReq = null;
        if(node.has("solicitudMedicamento")){
            JsonNode solMed = node.get("solicitudMedicamento");
            boolean solMedValid = HapiFhirUtils.validateObjectInJsonNode("solicitudMedicamento", solMed,oo,false);
            if (solMedValid) {
                medReq = mrt.transform(solMed, oo);
                if(patient != null) {
                    medReq.setSubject(new Reference(patient));
                }
                if (encounter != null) {
                    medReq.setEncounter(new Reference(encounter));
                }
            }
        }

        if(medReq != null) {
            CarePlan.CarePlanActivityComponent carePlanActivityComponent = new CarePlan.CarePlanActivityComponent();
            carePlanActivityComponent.setReference(new Reference(medReq));
            assert careplan != null;
            careplan.addActivity(carePlanActivityComponent);
        }
        if(examenSolicitados != null) {
            for (ServiceRequest examenSolicitado : examenSolicitados) {
                CarePlan.CarePlanActivityComponent carePlanActivityComponent = new CarePlan.CarePlanActivityComponent();
                carePlanActivityComponent.setReference(new Reference(examenSolicitado));
                if (careplan != null) careplan.addActivity(carePlanActivityComponent);
            }
        }


        if (!oo.getIssue().isEmpty()){
            res = HapiFhirUtils.resourceToString(oo,fhirServerConfig.getFhirContext());
            return res;
        }

        HapiFhirUtils.addResourceToBundle(b, messageHeader);
        HapiFhirUtils.addResourceToBundle(b, sr,srFullUrl);
        HapiFhirUtils.addResourceToBundle(b,practitioner);
        HapiFhirUtils.addResourceToBundle(b,atendedor);
        HapiFhirUtils.addResourceToBundle(b,organization);
        if(medReq != null)
            HapiFhirUtils.addResourceToBundle(b,medReq);
        HapiFhirUtils.addResourceToBundle(b,cond);
        if(anam != null)
            HapiFhirUtils.addResourceToBundle(b,anam);
        for(AllergyIntolerance aler : alergias){
            HapiFhirUtils.addResourceToBundle(b,aler);
        }
        if(examenSolicitados != null) {
            for (ServiceRequest s : examenSolicitados) {
                s.setSubject(new Reference(patient));
                s.getBasedOn().add(new Reference(sr));
                s.setRequester(new Reference(practitioner));
                HapiFhirUtils.addResourceToBundle(b, s);
                s.addIdentifier(new Identifier().setValue(s.getIdPart()));
            }
        }
        if(examenes != null) {
            for (Observation ob : examenes) {
                ob.setSubject(new Reference(patient));
                ob.setEncounter(new Reference(encounter));
                HapiFhirUtils.addResourceToBundle(b, ob);
            }
        }

        HapiFhirUtils.addResourceToBundle(b,encounter);
        HapiFhirUtils.addResourceToBundle(b,careplan);

        setMessageHeaderReferences(messageHeader, new Reference(sr), new Reference(atendedor), new Reference(encounter));

        res = HapiFhirUtils.resourceToString(b, fhirServerConfig.getFhirContext());
        return res;
    }
    
    
    public void setMessageHeaderReferences(MessageHeader m, Reference sr, Reference pr, Reference encR){
        m.setAuthor(pr);
        m.getFocus().add(sr);
        m.getFocus().add(encR);
    }
    
    
    public ServiceRequest buildServiceRequest(JsonNode node, OperationOutcome oo){
        String profile ="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ServiceRequestLE";
        ServiceRequest sr = new ServiceRequest();
        sr.getMeta().addProfile(profile);

        sr.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        String id = HapiFhirUtils.readStringValueFromJsonNode("idSolicitudServicio", node);
        if(id!=null)
            sr.setId(id);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC.idSolicitudServicio", oo);

        try {
            Date d = HapiFhirUtils.readDateValueFromJsonNode("fechaSolicitudIC", node);
            sr.setAuthoredOn(d);
        } catch (ParseException ex) {
            Logger.getLogger(BundleAtenderTransformer.class.getName()).log(Level.SEVERE, null, ex);
            HapiFhirUtils.addErrorIssue("fechaSolicitudIC", ex.getMessage(), oo);
        }
        String modalidadAtencion = HapiFhirUtils.readStringValueFromJsonNode("modalidadAtencion", node);
        Coding coding = VSModalidadAtencionEnum.fromCode(modalidadAtencion).getCoding();
        sr.getCategoryFirstRep().addCoding(coding);

        String idIC = HapiFhirUtils.readStringValueFromJsonNode("idInterconsulta", node);
        if (idIC == null) HapiFhirUtils.addNotFoundIssue("idInterconsulta", oo);
        Identifier identifierIC = new Identifier().setValue(idIC);
        sr.addIdentifier(identifierIC);

        String codigoEstadoIC = "6";
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

            Extension extensionEstadoIC = new Extension(
                    "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEstadoInterconsultaCodigoLE",cc);
            sr.addExtension(extensionEstadoIC);

        return sr;
    }
}
