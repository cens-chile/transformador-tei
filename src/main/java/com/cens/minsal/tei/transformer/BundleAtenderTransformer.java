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
        ((ObjectNode)get).put("tipoEvento", "atender");

        if(get!=null)
            messageHeader = 
                messageHeaderTransformer.transform(get, oo);
        else
            HapiFhirUtils.addNotFoundIssue("datosSistema", oo);


        get = node.get("solicitudIC");
        ServiceRequest sr = null;
        if(get!=null)
            sr = buildServiceRequest(node, oo);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC", oo);


        // Prestador
        get = node.get("prestador");
        Practitioner practitioner = null;

        String tipoPrestador = HapiFhirUtils.readStringValueFromJsonNode("tipoPrestador", get);
        if (tipoPrestador != null) {
            if (!tipoPrestador.toLowerCase().equals("profesional") && !tipoPrestador.toLowerCase().equals("administrativo")) {
                HapiFhirUtils.addErrorIssue("Prestador.tipoPrestador", "Dato no válido", oo);
            }
        }else HapiFhirUtils.addNotFoundIssue("prestador.tipoPrestador",oo);

        practitioner = practitionerTransformer.transform(tipoPrestador, get, oo);

        get = node.get("establecimiento");
        Organization organization = null;
        if(get != null){
            organization = organizationTransformer.transform(get, oo,"");
        } else {
            HapiFhirUtils.addNotFoundIssue("No se encontraron datos de la organización(establecimiento)", oo);
        }

        // Rol del Profesional (practitionerRol)
        get = node.get("rolDelProfesional");
        PractitionerRole practitionerRole = null;
        if(get != null){
            practitionerRole = practitionerRoleTransformer.transform(get, oo);
        } else {
            HapiFhirUtils.addNotFoundIssue("Rol de profesional no definido", oo);
        }
        Coding roleCode = new Coding("https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSPractitionerTipoRolLE", "atendedor", "Atendedor");
        CodeableConcept cc = new CodeableConcept(roleCode);
        practitionerRole.addCode(cc);

        try {
            practitionerRole.setPractitioner(new Reference( practitioner));
        }catch (Exception e){
            HapiFhirUtils.addNotFoundIssue("Practitioner", oo);
        }


         get = node.get("paciente");
         Patient patient = null;
         if(get != null){
            PatientTransformer pt = new PatientTransformer(validator);
             patient = pt.transform(get, oo);
         } else HapiFhirUtils.addNotFoundIssue("paciente", oo);

        AllergyIntolerance allergyIntolerance  = new AllergyIntolerance();
        AllergyIntoleranceTransformer at = new AllergyIntoleranceTransformer(validator);

        List<AllergyIntolerance> alergias = new ArrayList();
        if(node.has("alergias")){
            JsonNode allNode = node.get("alergias");
            if(allNode!=null){
                alergias = at.transform(allNode, oo);
            }
         }

        for (AllergyIntolerance a: alergias){
            a.setPatient(new Reference(patient));
        }

        //Se agrega exámen solicitado
        List<ServiceRequest> examenSolicitados= serTransformer.buildSolicitudExamen(node, oo);

        //Se agregan resultados exámenes realizados
        List<Observation> examenes = new ArrayList();
        JsonNode resultados = node.get("resultadoExamenes");
        if(resultados!=null){
            ObservationTransformer observationTransformer = new ObservationTransformer(validator);
            examenes = observationTransformer.buildResultadoExamen(resultados, oo);
        }


        //Agrega recursos con sus respectivos UUID al bundle de respuesta
        HapiFhirUtils.addResourceToBundle(b, messageHeader);
        PractitionerRole resolutor = practitionerRoleTransformer.buildPractitionerRole("atendedor", organization, practitioner);
        String srFullUrl = HapiFhirUtils.getUrlBaseFullUrl()+"/ServiceRequest/"+sr.getId();
        HapiFhirUtils.addResourceToBundle(b, sr,srFullUrl);

        String refPat = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente", node);
        sr.getSubject().setReference(refPat);

        sr.getPerformer().add(new Reference(resolutor));

        HapiFhirUtils.addResourceToBundle(b,patient);
        HapiFhirUtils.addResourceToBundle(b,practitioner);
        HapiFhirUtils.addResourceToBundle(b,resolutor);
        HapiFhirUtils.addResourceToBundle(b,organization);

        //Encuentro

        Encounter encounter = encounterTransformer.transform(node, oo,"atender");
        encounter.setSubject(new Reference(patient));
        encounter.addBasedOn(new Reference(sr));
        encounter.setServiceProvider(new Reference(organization));

        //Construir Diagnostico
        ConditionTransformer conditionTransformer = new ConditionTransformer(validator);
        Condition cond = null;
        if(node.get("diagnostico")!=null){
            cond = conditionTransformer.transform(node.get("diagnostico"), oo,"diagnostico");
            cond.setSubject(new Reference(patient));
            //cond.setEncounter(new Reference(encounter));
        }
        else
            HapiFhirUtils.addNotFoundIssue("diagnostico", oo);


        encounter.addDiagnosis(new Encounter.DiagnosisComponent(new Reference(cond)));

        MedicationRequestTransformer mrt = new MedicationRequestTransformer(validator);
        MedicationRequest medReq = null;
        if(node.has("solicitudMedicamento")){
            medReq = mrt.transform(node.get("solicitudMedicamento"), oo);
            medReq.setSubject(new Reference(patient));
            medReq.setEncounter(new Reference(encounter));
            HapiFhirUtils.addResourceToBundle(b,medReq);
        }

      ObservationAnamnesisTransformer anamnesisTransformer = new ObservationAnamnesisTransformer(validator);
        Observation anam = null;

        if(node.has("anamnesis")){
            anam = anamnesisTransformer.transform(node.get("anamnesis"),oo);
            anam.setSubject(new Reference(patient));
            anam.setEncounter(new Reference(encounter));
            HapiFhirUtils.addResourceToBundle(b,anam);
        }

        HapiFhirUtils.addResourceToBundle(b,cond);

        for(AllergyIntolerance aler : alergias){
            HapiFhirUtils.addResourceToBundle(b,aler);
        }


        for(ServiceRequest s : examenSolicitados){
            s.setSubject(new Reference(patient));
            s.getBasedOn().add(new Reference(sr));
            s.setRequester(new Reference(practitioner));
            HapiFhirUtils.addResourceToBundle(b,s);
            s.addIdentifier(new Identifier().setValue(s.getIdPart()));
        }

        for(Observation ob : examenes){
            ob.setSubject(new Reference(patient));
            ob.setEncounter(new Reference(encounter));
            HapiFhirUtils.addResourceToBundle(b,ob);
        }

        HapiFhirUtils.addResourceToBundle(b,encounter);

// Plan de atencion -Careplan
        get = node.get("planDeAtencion");
        CarePlan  careplan = null;

        if(get != null){
            careplan = carePlanTransformer.transform(get, oo);

            if(patient != null){
                careplan.setSubject(new Reference(patient));
            }else{
                HapiFhirUtils.addNotFoundIssue("planDeAtencion.referenciaPaciente",oo);
            }

            if(encounter != null) {
                careplan.setEncounter(new Reference(encounter));
            }
        }

        //  - recetas (MedicationRequest)
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
        String descripcionPlan = HapiFhirUtils.readStringValueFromJsonNode("descripcion",get);
        if(descripcionPlan == null){
            HapiFhirUtils.addNotFoundIssue("No se encontró Descripción del Plan de Atención", oo);
        } else{
            careplan.setDescription(descripcionPlan);
        }

        if(careplan != null){
            HapiFhirUtils.addResourceToBundle(b,careplan);
        }else HapiFhirUtils.addErrorIssue("Careplan","Plan de cuidados no puede ser vacio",oo );

        setMessageHeaderReferences(messageHeader, new Reference(sr), new Reference(resolutor), new Reference(encounter));

        if (!oo.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(oo,fhirServerConfig.getFhirContext());
            return res;
        }

        res = HapiFhirUtils.resourceToString(b, fhirServerConfig.getFhirContext());
        return res;
    }
    
    
    public void setMessageHeaderReferences(MessageHeader m, Reference sr, Reference pr, Reference encR){
        m.setAuthor(pr);
        m.getFocus().add(sr);
        m.getFocus().add(encR);
    }
    
    
    public ServiceRequest buildServiceRequest(JsonNode nodeOrigin, OperationOutcome oo){
        String profile ="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ServiceRequestLE";
        ServiceRequest sr = new ServiceRequest();
        sr.getMeta().addProfile(profile);
        JsonNode node = nodeOrigin.get("solicitudIC");

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

        Reference pacienteRef = new Reference(HapiFhirUtils.readStringValueFromJsonNode(
                "referenciaPaciente",nodeOrigin));
        if(pacienteRef != null ){
            sr.setSubject(pacienteRef);
        }else HapiFhirUtils.addNotFoundIssue("referenciaPaciente(para solicitudIC)",oo);

        JsonNode practitionerJ = nodeOrigin.get("prestador");

        if(practitionerJ == null){
            HapiFhirUtils.addNotFoundIssue("prestador", oo);
        }
        try {
            String idPractitioner = HapiFhirUtils.readStringValueFromJsonNode("id", practitionerJ);
            if (practitionerJ != null && idPractitioner != null) {
                List<Reference> practL = new ArrayList<>();
                Reference practitionerRef = new Reference("Practitioner/" + idPractitioner);
                practL.add(practitionerRef);
                //sr.setPerformer(practL);
            } else HapiFhirUtils.addNotFoundIssue("Prestador.id",oo);

        } catch (Exception e) {
            HapiFhirUtils.addNotFoundIssue("practitioner.id", oo);
        }

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
