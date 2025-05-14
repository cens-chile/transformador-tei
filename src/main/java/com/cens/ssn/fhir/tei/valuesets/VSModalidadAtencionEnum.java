/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.ssn.fhir.tei.valuesets;

import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.Coding;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
public enum VSModalidadAtencionEnum {
    
    PRESENCIAL("1"),REMOTA("2"),TELEMEDICINA("3");
    
    private String code;
    private static Map<String, VSModalidadAtencionEnum> VSModalidadAtencionEnum = null;
    private static String system = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSModalidadAtencionCodigo";

    VSModalidadAtencionEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
    public String toDisplay() {
        switch (this.code) {
            case "1" -> {
                return "Presencial";
            }
            case "2" -> {
                return "Remota";
            }
            case "3" -> {
                return "Telemedicina";
            }
            default -> {
            }
        }
        return null;
    }

    public static VSModalidadAtencionEnum fromCode(String theCode) {
        Map<String, VSModalidadAtencionEnum> c2s = VSModalidadAtencionEnum;
        if (c2s == null) {
            c2s = new HashMap<>();
            for (VSModalidadAtencionEnum next : values()) {
                c2s.put(next.getCode(), next);
            }
            VSModalidadAtencionEnum = c2s;
        }
        return c2s.get(theCode);
    }

    public static String toCode(String entry) {
        switch (entry) {
            case "Presencial" -> {
                return "1";
            }
            case "Remota" -> {
                return "2";
            }
            case "Telemedicina" -> {
                return "3";
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
