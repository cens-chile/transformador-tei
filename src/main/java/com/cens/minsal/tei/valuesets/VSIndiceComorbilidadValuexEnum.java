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
public enum VSIndiceComorbilidadValuexEnum {
    
    SIN_RIESGO("G0"),RIESGO_LEVE("G1"),RIESGO_MODERADO("G2"),
    RIESGO_ALTO("G3");
    
    private String code;
    private static Map<String, VSIndiceComorbilidadValuexEnum> vsIndiceComorbilidadEnum = null;
    private static String system = "https://interoperabilidad.minsal.cl/fhir/ig/tei/CodeSystem/CSIndicecomorbilidad";

    VSIndiceComorbilidadValuexEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
    public String toDisplay() {
        switch (this.code) {
            case "G0" -> {
                return "Sin Riesgo, sin condiciones crónicas o riesgo no identificado";
            }
            case "G1" -> {
                return "Riesgo Leve, 1 condición crónica";
            }
            case "G2" -> {
                return "Riesgo Moderado, 2 a 4 condiciones crónicas";
            }
            case "G3" -> {
                return "Riesgo Alto, 5 o más condiciones crónicas";
            }
            default -> {
            }
        }
        return null;
    }

    public static VSIndiceComorbilidadValuexEnum fromCode(String theCode) {
        Map<String, VSIndiceComorbilidadValuexEnum> c2s = vsIndiceComorbilidadEnum;
        if (c2s == null) {
            c2s = new HashMap<>();
            for (VSIndiceComorbilidadValuexEnum next : values()) {
                c2s.put(next.getCode(), next);
            }
            vsIndiceComorbilidadEnum = c2s;
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
