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
public enum VSDerivadoParaEnum {
    
    CONFIRMACION("1"),CONTROL_ESPECIALISTA("2"),REALIZA_TRATAMIENTO("3"),
    SEGUIMIENTO("4"),OTRO("5");
    
    private String code;
    private static Map<String, VSDerivadoParaEnum> vsDerivadoParaEnum = null;
    private static String system = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSDerivadoParaCodigo";

    VSDerivadoParaEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
    public String toDisplay() {
        switch (this.code) {
            case "1" -> {
                return "Confirmación";
            }
            case "2" -> {
                return "Control especialista";
            }
            case "3" -> {
                return "Realiza tratamiento";
            }
            case "4" -> {
                return "Seguimiento";
            }
            case "5" -> {
                return "Otro";
            }
            default -> {
            }
        }
        return null;
    }

    public static VSDerivadoParaEnum fromCode(String theCode) {
        Map<String, VSDerivadoParaEnum> c2s = vsDerivadoParaEnum;
        if (c2s == null) {
            c2s = new HashMap<>();
            for (VSDerivadoParaEnum next : values()) {
                c2s.put(next.getCode(), next);
            }
            vsDerivadoParaEnum = c2s;
        }
        return c2s.get(theCode);
    }

    public static String toCode(String entry) {
        switch (entry) {
            case "Confirmación" -> {
                return "1";
            }
            case "Control especialista" -> {
                return "2";
            }
            case "Realiza tratamiento" -> {
                return "3";
            }
            case "Seguimiento" -> {
                return "4";
            }
            case "Otro" -> {
                return "5";
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
