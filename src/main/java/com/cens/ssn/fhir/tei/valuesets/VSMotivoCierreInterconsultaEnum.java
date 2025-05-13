/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.ssn.fhir.tei.valuesets;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
public enum VSMotivoCierreInterconsultaEnum {
    
    GES("1"),
    ATENCION_RREALIZADA("2"),
    PROCEDIMIENTO_EJECUTADO("3"),
    ATENCION_OTOGADA_ES("4"),
    NO_BENEFICIARIO("5"),
    RENUNCIA_RECHAZO("6"),
    RECUPERACION_ESPONTANEA("7"),
    INASISTENCIA("8"),
    FALLECIMIENTO("9"),
    SOLICITUD_DUPLICADA("10"),
    CONTACTO_NO_CORRESPONDE("11"),
    TRASLADO_COORDINADO("12"),
    NO_PERTINENCIA("13"),
    ERROR_DIGITACION("14"),
    ATENCION_POR_RESOLUTIVIDAD("15"),
    ATENCION_TELEMEDICINA("16"),
    MODIFICACIION_CONDICION_CLINICA("17"),
    ATENCION_HOSP_DIGITAL("18");

    private String code;
    private static Map<String, VSMotivoCierreInterconsultaEnum> ourCodeToUrgencyLevel = null;

    VSMotivoCierreInterconsultaEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static VSMotivoCierreInterconsultaEnum fromCode(String theCode) {
        Map<String, VSMotivoCierreInterconsultaEnum> c2s = ourCodeToUrgencyLevel;
        if (c2s == null) {
            c2s = new HashMap<>();
            for (VSMotivoCierreInterconsultaEnum next : values()) {
                c2s.put(next.getCode(), next);
            }
            ourCodeToUrgencyLevel = c2s;
        }
        return c2s.get(theCode);
    }

    public static String toCode(String entry) {
        switch (entry) {
            case "GES (0)" -> {
                return "1";
            }
            case "Atención Realizada (1)" -> {
                return "2";
            }
            case "Corresponde a la realización del examen procedimiento ejecutado (2)" -> {
                return "3";
            }
            case "Atención Otorgada en el Extra sistema (4)" -> {
                return "4";
            }
            case "No beneficiario (5)" -> {
                return "5";
            }
            case "Renuncia o rechazo voluntario (6)" -> {
                return "6";
            }
            case "Recuperación espontánea (7)" -> {
                return "7";
            }
            case "Inasistencia (2 NSP) (8)" -> {
                return "8";
            }
            case "Fallecimiento (9)" -> {
                return "9";
            }
            case "Solicitud de indicación duplicada (10)" -> {
                return "10";
            }
            case "Contacto no corresponde (11)" -> {
                return "11";
            }
            case "Traslado coordinado (12)" -> {
                return "12";
            }
            case "No pertinencia (13)" -> {
                return "13";
            }
            case "Error de digitación(15)" -> {
                return "14";
            }
            case "Atención por resolutividad (16)" -> {
                return "1";
            }
            case "Atención por telemedicina (17)" -> {
                return "1";
            }
            case "Modificación de condicion clínica del paciente (18)" -> {
                return "1";
            }
            case "Atención hospital digital (19)" -> {
                return "1";
            }
            default -> {
            }
        }
        return null;
    }

    public static String getSystem() {
        return "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSMotivoCierreInterconsulta";
    }
    
}
