/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */

@ConfigurationProperties(prefix = "tei.fhir")
@Configuration
@EnableConfigurationProperties
public class AppProperties {
    
    private String validator_fhir_package_tei = null;
    private String validator_fhir_package_core = null;
    private String validator_fhir_package_tei_version = null;

    public String getValidator_fhir_package_tei() {
        return validator_fhir_package_tei;
    }

    public void setValidator_fhir_package_tei(String validator_fhir_package_tei) {
        this.validator_fhir_package_tei = validator_fhir_package_tei;
    }

    public String getValidator_fhir_package_core() {
        return validator_fhir_package_core;
    }

    public void setValidator_fhir_package_core(String validator_fhir_package_core) {
        this.validator_fhir_package_core = validator_fhir_package_core;
    }

    public String getValidator_fhir_package_tei_version() {
        return validator_fhir_package_tei_version;
    }

    public void setValidator_fhir_package_tei_version(String validator_fhir_package_tei_version) {
        this.validator_fhir_package_tei_version = validator_fhir_package_tei_version;
    }

    
    
}
