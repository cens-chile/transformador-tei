/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.valuesets;

import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.Coding;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
public enum VSEstadoInterconsultaEnum {
    
    ESPERA_REFERENCIA("1"),ESPERA_REVISION("2"),ESPERA_PRIORIZACION("3"),
    ESPERA_AGENDAMIENTO("4"),ESPERA_ATENCION("5"),ESPERA_CIERRE("6"),
    CERRADA("7");
    
    private String code;
    private static Map<String, VSEstadoInterconsultaEnum> vsEstadoInterconsultaEnum = null;
    private static String system = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSEstadoInterconsulta";

    VSEstadoInterconsultaEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
    public String toDisplay() {
        switch (this.code) {
            case "1" -> {
                return "A la espera de referencia";
            }
            case "2" -> {
                return "A la espera de revisión";
            }
            case "3" -> {
                return "A la espera de priorización";
            }
            case "4" -> {
                return "A la espera de agendamiento";
            }
            case "5" -> {
                return "A la espera de atención";
            }
            case "6" -> {
                return "A la espera de cierre";
            }
            case "7" -> {
                return "Cerrada";
            }
            default -> {
            }
        }
        return null;
    }

    public static VSEstadoInterconsultaEnum fromCode(String theCode) {
        Map<String, VSEstadoInterconsultaEnum> c2s = vsEstadoInterconsultaEnum;
        if (c2s == null) {
            c2s = new HashMap<>();
            for (VSEstadoInterconsultaEnum next : values()) {
                c2s.put(next.getCode(), next);
            }
            vsEstadoInterconsultaEnum = c2s;
        }
        return c2s.get(theCode);
    }

    public static String getSystem() {
        return system;
    }
    
    public Coding getCoding(){
        
        Coding c = new Coding(system,getCode(),toDisplay());
        return c;
    }
}
