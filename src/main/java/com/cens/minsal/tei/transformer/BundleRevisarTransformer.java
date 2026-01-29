/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.transformer;

import com.cens.minsal.tei.config.FhirServerConfig;
import com.cens.minsal.tei.services.ValueSetValidatorService;
import com.cens.minsal.tei.utils.HapiFhirUtils;
import com.cens.minsal.tei.utils.JsonUniqueKeyValidator;
import com.cens.minsal.tei.valuesets.VSDerivadoParaEnum;
import com.cens.minsal.tei.valuesets.VSEstadoInterconsultaEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Component
public class BundleRevisarTransformer {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BundleRevisarTransformer.class);
    FhirServerConfig fhirServerConfig;
    static final String bundleProfile="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/BundleRevisarLE";
    static final String snomedSystem = "http://snomed.info/sct";
    PatientTransformer patientTr;
    MessageHeaderTransformer messageHeaderTransformer;
    EncounterTransformer encTransformer;
    OrganizationTransformer orgTransformer;
    AllergyIntoleranceTransformer allInTransformer;
    QuestionnaireResponseTransformer questTransformer;
    ServiceRequestTransformer serTransformer;
    PractitionerTransformer praTransformer;
    PractitionerRoleTransformer referenciadorTransformer;
    ValueSetValidatorService validator;
    
    public BundleRevisarTransformer(FhirServerConfig fhirServerConfig,
            MessageHeaderTransformer messageHeaderTransformer,
            EncounterTransformer encTransformer,
            PatientTransformer patientTr,
            OrganizationTransformer orgTransformer,
            ValueSetValidatorService validator,
            QuestionnaireResponseTransformer questTransformer,
            AllergyIntoleranceTransformer allInTransformer,
            ServiceRequestTransformer serTransformer,
            PractitionerTransformer praTransformer,
            PractitionerRoleTransformer referenciadorTransformer) {
        this.fhirServerConfig = fhirServerConfig;
        this.messageHeaderTransformer = messageHeaderTransformer;
        this.encTransformer = encTransformer;
        this.patientTr = patientTr;
        this.orgTransformer = orgTransformer;
        this.allInTransformer = allInTransformer;
        this.questTransformer = questTransformer;
        this.serTransformer = serTransformer;
        this.praTransformer = praTransformer;
        this.referenciadorTransformer = referenciadorTransformer;
        this.validator = validator;
    }
    
    
    public String buildBundle(String cmd){
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
            java.util.logging.Logger.getLogger(BundleRevisarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            throw new FHIRException(ex.getMessage());
        }
        
        JsonNode get = node.get("datosSistema");
        MessageHeader messageHeader = null;

        boolean validate = HapiFhirUtils.validateObjectInJsonNode("datosSistema", get, out,true);
        if(validate){
            ((ObjectNode)get).put("tipoEvento", "revisar");
            messageHeader = messageHeaderTransformer.transform(node.get("datosSistema"), out);
        }

        JsonNode paciente = node.get("paciente");
        
        String refPatText = HapiFhirUtils.readStringValueFromJsonNode("referenciaPaciente", node);
        Reference patRef = null;
        if(refPatText!=null)
            patRef= new Reference(refPatText);   
        else
            HapiFhirUtils.addNotFoundIssue("referenciaPaciente", out);

        //referenciaAtendedor
        String refAtendedorText = HapiFhirUtils.readStringValueFromJsonNode("IDRolAtendedor", node);
        if(refAtendedorText ==null) {
            HapiFhirUtils.addNotFoundIssue("IDRolAtendedor", out);
        }

        //Se construye Prestador
        get = node.get("prestadorRevisor");
        validate = HapiFhirUtils.validateObjectInJsonNode("prestadorRevisor", get, out,true);
        Practitioner practitioner = null;
        if(validate)
                practitioner = praTransformer.transform("profesional", get, out);
       
                
        
        get = node.get("solicitudIC");
        validate = HapiFhirUtils.validateObjectInJsonNode("solicitudIC", get, out,true);
        ServiceRequest sr = null;
        if(validate)
            sr = buildServiceRequest(get, out);  

        
        Organization org = null;
        Organization orgDest = null;
        JsonNode establecimientos = node.get("establecimiento");
        validate = HapiFhirUtils.validateObjectInJsonNode("establecimiento", establecimientos, out,true);
        if(validate){
            //Construir Organización que revisa
            get = establecimientos.get("origen");
            validate = HapiFhirUtils.validateObjectInJsonNode("establecimiento.origen", get, out,true);
            if(validate)
                org = orgTransformer.transform(get, out,"establecimiento.origen");
            //Construir Organización de destino
            get = establecimientos.get("destino");
            validate = HapiFhirUtils.validateObjectInJsonNode("establecimiento.destino", get, out,true);
            if(validate)
                orgDest = orgTransformer.transform(get, out,"establecimiento.destino");
            
        }
        
        //Se agrega exámen solicitado
        List<ServiceRequest> examenSolicitados= serTransformer.buildSolicitudExamenList(node, out);
        
        if (!out.getIssue().isEmpty()) {
            res = HapiFhirUtils.resourceToString(out,fhirServerConfig.getFhirContext());
            return res;
        }

        PractitionerRole revisor = referenciadorTransformer.buildPractitionerRole("revisor", org, practitioner);
        PractitionerRole resolutor = referenciadorTransformer.buildPractitionerRole("atendedor", orgDest, null);
        
        HapiFhirUtils.addResourceToBundle(b, messageHeader);
        setMessageHeaderReferences(messageHeader, new Reference(sr), new Reference(revisor));
        
        String srFullUrl = HapiFhirUtils.getUrlBaseFullUrl()+"/ServiceRequest/"+sr.getId();
        HapiFhirUtils.addResourceToBundle(b, sr,srFullUrl);
        sr.setSubject(patRef);

        sr.getPerformer().add(new Reference(resolutor));

        HapiFhirUtils.addResourceToBundle(b, practitioner);
        
        HapiFhirUtils.addResourceToBundle(b, org);
        
        HapiFhirUtils.addResourceToBundle(b, orgDest);
        
        
        HapiFhirUtils.addResourceToBundle(b,revisor);
        resolutor.setId(refAtendedorText);
        HapiFhirUtils.addResourceToBundle(b, resolutor);

        if(examenSolicitados != null) {
            if (!examenSolicitados.isEmpty()) {
                for (ServiceRequest s : examenSolicitados) {
                    HapiFhirUtils.addResourceToBundle(b, s);
                    s.setSubject(patRef);
                    s.getBasedOn().add(new Reference(sr));
                    s.setRequester(new Reference(practitioner));
                }
            }
        }
        
        res = HapiFhirUtils.resourceToString(b, fhirServerConfig.getFhirContext());

        return res;
    }
    
    
    public void setMessageHeaderReferences(MessageHeader m, Reference sr, Reference pr){
        m.setAuthor(pr);
        m.getFocus().add(sr);
    }
    
    
    public ServiceRequest buildServiceRequest(JsonNode node, OperationOutcome oo){
        String profile ="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ServiceRequestLE";
        ServiceRequest sr = new ServiceRequest();
        sr.getMeta().addProfile(profile);
        
        String id = HapiFhirUtils.readStringValueFromJsonNode("idSolicitudServicio", node);
        if(id!=null)
            sr.setId(id);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC.idSolicitudServicio", oo);


        String estadoICcodigo = HapiFhirUtils.readStringValueFromJsonNode("estadoICcodigo", node);
        if(estadoICcodigo!=null) {
            String cs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEstadoInterconsulta";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSEstadoInterconsulta";
            String validateCode = validator.validateCode(cs, estadoICcodigo, null, vs);
            if(validateCode!=null){
                Coding c = new Coding(cs,estadoICcodigo,validateCode);
                String extUrl = "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEstadoInterconsultaCodigoLE";
                Extension buildExtension = HapiFhirUtils.buildExtension(extUrl,new CodeableConcept(c));
                sr.getExtension().add(buildExtension);
            }
            else
                HapiFhirUtils.addInvalidIssue("solicitudIC.estadoICcodigo", oo);
        }
        else {
            HapiFhirUtils.addNotFoundIssue("solicitudIC.estadoICcodigo", oo);
        }

        try {
            String d = HapiFhirUtils.readDateTimeValueFromJsonNode("fechaSolicitudIC", node);
            if (HapiFhirUtils.isValidDateFormat(d)){
                sr.getAuthoredOnElement().setValueAsString(d);
            }
        } catch (ParseException ex) {
            Logger.getLogger(BundleIniciarTransformer.class.getName()).log(Level.SEVERE, null, ex);
            HapiFhirUtils.addErrorIssue("fechaSolicitudIC", ex.getMessage(), oo);
        }

        String iden = HapiFhirUtils.readStringValueFromJsonNode("idInterconsulta", node);
        if(iden!=null)
            sr.getIdentifierFirstRep().setValue(iden);
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC.idInterconsulta", oo);



        sr.setStatus(ServiceRequest.ServiceRequestStatus.DRAFT);
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        
       
        String modalidadAtencion = HapiFhirUtils.readStringValueFromJsonNode("modalidadAtencion", node);
        if(modalidadAtencion!=null){
            //VSModalidadAtencionEnum fromCode = VSModalidadAtencionEnum.fromCode(modalidadAtencion);
            String cs ="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSModalidadAtencionCodigo";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSModalidadAtencionCodigo";
            String validateCode = validator.validateCode(cs, modalidadAtencion, null, vs);
            if(validateCode!=null){
                Coding c = new Coding(cs,modalidadAtencion,validateCode);
                sr.getCategoryFirstRep().addCoding(c);
            }
            else
                HapiFhirUtils.addInvalidIssue("solicitudIC.modalidadAtencion", oo);
        }
        
        String derivadoPara = HapiFhirUtils.readStringValueFromJsonNode("derivadoPara", node);
        if(derivadoPara!=null){
            VSDerivadoParaEnum fromCode = VSDerivadoParaEnum.fromCode(derivadoPara);
            if(fromCode!=null){
                Coding coding = VSDerivadoParaEnum.fromCode(derivadoPara).getCoding();
                sr.getReasonCodeFirstRep().addCoding(coding);
            } 
            else
                HapiFhirUtils.addErrorIssue("derivadoPara","código no encontrado", oo);
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
        
        
        String destinoAtencion = HapiFhirUtils.readStringValueFromJsonNode("destinoAtencion", node);
        if(destinoAtencion!=null){
            String cs ="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSDestinoReferenciaCodigo";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSDestinoReferenciaCodigo";
            String validateCode = validator.validateCode(cs, destinoAtencion, null, vs);
            if(validateCode!=null){
                Coding c = new Coding(cs,destinoAtencion,validateCode);
                sr.getLocationCodeFirstRep().addCoding(c);
            }
            else
                HapiFhirUtils.addInvalidIssue("solicitudIC.destinoAtencion", oo);
        }else
            HapiFhirUtils.addNotFoundIssue("solicitudIC.destinoAtencion", oo);
       
        Boolean atPreferente = HapiFhirUtils.readBooleanValueFromJsonNode("atencionPreferente", node);
        if(atPreferente!=null){
            if(!node.get("atencionPreferente").isBoolean())
                HapiFhirUtils.addInvalidIssue("atencionPreferente", oo);
            
            Extension extAtPreferente = 
                HapiFhirUtils.buildBooleanExt(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionBoolAtencionPreferente",
                 atPreferente);
            sr.addExtension(extAtPreferente);
            
        }
        
        Boolean resolutividadAPS = HapiFhirUtils.readBooleanValueFromJsonNode("resolutividadAPS", node);
        if(resolutividadAPS!=null){
            if(!node.get("resolutividadAPS").isBoolean())
                HapiFhirUtils.addInvalidIssue("solicitudIC.resolutividadAPS", oo);
            
            Extension extResolutividadAPS = 
                HapiFhirUtils.buildBooleanExt(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionBoolResolutividadAPS",
                 resolutividadAPS);
            sr.addExtension(extResolutividadAPS);
            
        }
        
        String fundamentoPri = HapiFhirUtils.readStringValueFromJsonNode("fundamentoPriorizacion", node);
        
        Extension ext = HapiFhirUtils.buildExtension(
                "https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionStringFundamentoPriorizacion"
                , new StringType(fundamentoPri));
        sr.addExtension(ext);

        //EspecialidadMédicaDestinoCódigo
        JsonNode especialidad = node.get("especialidadMedicaDestino");
        if(especialidad!=null){
            String cs ="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VsEspecialidadDest";
            String get = HapiFhirUtils.readStringValueFromJsonNode("tipo", especialidad);
            if(get!=null){
                if(get.equals("odontologica"))
                    cs="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadOdont";
                else if(!get.equals("medica"))
                    HapiFhirUtils.addErrorIssue("solicitudIC.especialidadMedicaDestino.tipo",
                            "No se conoce el tipo de especialidad", oo);
            }     
            get = HapiFhirUtils.readStringValueFromJsonNode("codigo", especialidad);
            if(get!=null){
                String validateCode = validator.validateCode(cs, get, null, vs);
                if(validateCode!=null){
                    String extUrl ="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionEspecialidadMedicaDestinoCodigo";
                    Coding c = new Coding(cs,get,validateCode);
                    Extension buildExtension = HapiFhirUtils.buildExtension(extUrl,new CodeableConcept(c));
                    sr.getExtension().add(buildExtension);
                }
                else
                    HapiFhirUtils.addNotFoundCodeIssue("solicitudIC.especialidadMedicaDestino.codigo", oo);
            }else
                HapiFhirUtils.addNotFoundIssue("solicitudIC.especialidadMedicaDestino.codigo", oo);
        }
        else
            HapiFhirUtils.addNotFoundIssue("solicitudIC.especialidadMedicaDestino", oo);
        
        //EspecialidadMédicaDestinoCódigo
        JsonNode subEspecialidad = node.get("subEspecialidadMedicaDestino");
        if(subEspecialidad!=null){
            String cs ="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadMed";
            String vs = "https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VsEspecialidadDest";
            String get = HapiFhirUtils.readStringValueFromJsonNode("tipo", subEspecialidad);
            if(get!=null){
                if(get.equals("odontologica"))
                    cs="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEspecialidadOdont";
                else if(!get.equals("medica"))
                    HapiFhirUtils.addErrorIssue("solicitudIC.subEspecialidadMedicaDestino.tipo",
                            "No se conoce el tipo de especialidad", oo);
            }     
            get = HapiFhirUtils.readStringValueFromJsonNode("codigo", subEspecialidad);
            if(get!=null){
                String validateCode = validator.validateCode(cs, get, null, vs);
                if(validateCode!=null){
                    String extUrl ="https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionSubEspecialidadMedicaDestinoCodigo";
                    Coding c = new Coding(cs,get,validateCode);
                    Extension buildExtension = HapiFhirUtils.buildExtension(extUrl,new CodeableConcept(c));
                    sr.getExtension().add(buildExtension);
                }
                else
                    HapiFhirUtils.addNotFoundCodeIssue("solicitudIC.subEspecialidadMedicaDestino.codigo", oo);
            }else
                HapiFhirUtils.addNotFoundIssue("solicitudIC.subEspecialidadMedicaDestino.codigo", oo);
        }
        
        //Se agrega pertinencia de la interconsulta
        Extension pertinencia = new Extension("https://interoperabilidad.minsal.cl/fhir/ig/tei/StructureDefinition/ExtensionPertinenciaInterconsulta");
        JsonNode pertinenciaNode = node.get("pertinenciaIC");
        if(pertinenciaNode!=null){
            String codigoPertinencia = HapiFhirUtils.readStringValueFromJsonNode("codigo", pertinenciaNode);
            if(codigoPertinencia!=null){
                String vsPer="https://interoperabilidad.minsal.cl/fhir/ig/tei/ValueSet/VSPertinenciaInterconsulta";
                String csPer="https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSPertinenciaInterconsulta";
                String res = validator.validateCode(csPer, codigoPertinencia,"",vsPer);
                if(res!=null){
                    CodeableConcept c = new CodeableConcept();
                    Coding cod=new Coding(csPer,codigoPertinencia,res);
                    c.addCoding(cod);
                    Extension perCodeExt = HapiFhirUtils.buildExtension("EvaluacionPertinencia",c);
                    pertinencia.addExtension(perCodeExt);
                    sr.getExtension().add(pertinencia);
                    if(codigoPertinencia.equals("2"))
                    {
                        String mot = HapiFhirUtils.readStringValueFromJsonNode("motivoNoPertinencia", pertinenciaNode);
                        if(mot!=null){
                            pertinencia.addExtension(HapiFhirUtils.
                                    buildExtension("MotivoNoPertinencia",new StringType(mot)));    
                        }
                        else
                            HapiFhirUtils.addNotFoundIssue("solicitudIC.pertinenciaIC.MotivoNoPertinencia", oo);
                    }
                }else
                    HapiFhirUtils.addNotFoundCodeIssue("solicitudIC.pertinenciaIC.codigo", oo);
                
            }else
                HapiFhirUtils.addNotFoundIssue("solicitudIC.pertinenciaIC.codigo",oo);  
        }else
            HapiFhirUtils.addNotFoundCodeIssue("solicitudIC.pertinenciaIC",oo);
 
        return sr;
    }
  
    
    public void setServiceRequestReferences(ServiceRequest ser,Patient pat,Encounter enc,
        PractitionerRole requester,Condition diagSos,List<AllergyIntolerance> alls,
        Observation indiceComorbilidad,Observation cuidador, Observation dis,
        QuestionnaireResponse motDer,List<ServiceRequest> solExams){
        
        ser.setSubject(new Reference(pat));
        ser.setEncounter(new Reference(enc));
        ser.setRequester(new Reference(requester));
        ser.getSupportingInfo().add(new Reference(diagSos));
        alls.forEach(al -> {
            ser.getSupportingInfo().add(new Reference(al));
        });
        ser.getSupportingInfo().add(new Reference(indiceComorbilidad));
        ser.getSupportingInfo().add(new Reference(cuidador));
        ser.getSupportingInfo().add(new Reference(dis));
        ser.getSupportingInfo().add(new Reference(motDer));
        solExams.forEach(sol -> {
            ser.getSupportingInfo().add(new Reference(sol));
        });
        
    }
}
