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
public enum VSMessageHeaderEventEnum {
    
    INICIAR("iniciar"),REFERENCIAR("referenciar"),REVISAR("revisar"),
    PRIORIZAR("priorizar"),AGENDAR("agendar"),ATENDER("atender"),
    TERMINAR("terminar");
    
    private String code;
    private static Map<String, VSMessageHeaderEventEnum> ourCodeToMessageHeaderEvent = null;
    private static String system = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSTipoEventoLE";

    VSMessageHeaderEventEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
    public String toDisplay() {
        switch (this.code) {
            case "iniciar" -> {
                return "Iniciar";
            }
            case "referenciar" -> {
                return "Referenciar";
            }
            case "revisar" -> {
                return "Revisar";
            }
            case "priorizar" -> {
                return "Priorizar";
            }
            case "agendar" -> {
                return "Agendar";
            }
            case "atender" -> {
                return "Atender";
            }
            case "terminar" -> {
                return "Terminar";
            }
            default -> {
            }
        }
        return null;
    }

    public static VSMessageHeaderEventEnum fromCode(String theCode) {
        Map<String, VSMessageHeaderEventEnum> c2s = ourCodeToMessageHeaderEvent;
        if (c2s == null) {
            c2s = new HashMap<>();
            for (VSMessageHeaderEventEnum next : values()) {
                c2s.put(next.getCode(), next);
            }
            ourCodeToMessageHeaderEvent = c2s;
        }
        return c2s.get(theCode);
    }

    public static String toCode(String entry) {
        switch (entry) {
            case "Iniciar" -> {
                return "iniciar";
            }
            case "Referenciar" -> {
                return "referenciar";
            }
            case "Revisar" -> {
                return "revisar";
            }
            case "Priorizar" -> {
                return "priorizar";
            }
            case "Agendar" -> {
                return "agendar";
            }
            case "Atender" -> {
                return "atender";
            }
            case "Terminar" -> {
                return "terminar";
            }
            default -> {
            }
        }
        return null;
    }

    public static String getSystem() {
        return system;
    }
    
    public Coding getCoding(){
        
        Coding c = new Coding(system,getCode(),toDisplay());
        return c;
    }
}
