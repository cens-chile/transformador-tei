/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cens.minsal.tei.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport.IssueSeverity;
import java.io.IOException;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.UnknownCodeSystemWarningValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author José <jose.m.andrade@gmail.com>
 */
@Configuration
public class FhirTermsValidatorConfig {
    
    AppProperties properties;
    
    
    @Bean
    public IValidationSupport validationsupport(AppProperties appProperties) throws IOException {
      // Create an NPM Package Support module and load one package in from
      // the classpath
      FhirContext ctx = FhirContext.forR4Cached();
      NpmPackageValidationSupport npmPackageSupport = new NpmPackageValidationSupport(ctx);
        System.out.println("appProperties = " + appProperties.getValidator_fhir_package_tei());
      npmPackageSupport.loadPackageFromClasspath(
          "classpath:" + appProperties.getValidator_fhir_package_tei());
      npmPackageSupport.loadPackageFromClasspath(
          "classpath:" + appProperties.getValidator_fhir_package_core());
      // Create a support chain including the NPM Package Support
      UnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport = new UnknownCodeSystemWarningValidationSupport(
          ctx);
      unknownCodeSystemWarningValidationSupport.setNonExistentCodeSystemSeverity(
          IssueSeverity.ERROR);
      ValidationSupportChain validationSupportChain = new ValidationSupportChain(
          npmPackageSupport,
          new DefaultProfileValidationSupport(ctx),
          new PrePopulatedValidationSupport(ctx),
          new CommonCodeSystemsTerminologyService(ctx),
          new InMemoryTerminologyServerValidationSupport(ctx),
          new SnapshotGeneratingValidationSupport(ctx),
          unknownCodeSystemWarningValidationSupport);
      CachingValidationSupport validationSupport = new CachingValidationSupport(
          validationSupportChain);
      return validationSupport;
    }
    
}
