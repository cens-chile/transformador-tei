package com.cens.minsal.tei.utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.hl7.fhir.r4.model.OperationOutcome;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.DupDetector;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.OperationOutcome;

import java.io.IOException;


public class JsonUniqueKeyValidator {

    public static JsonNode validateUniqueKeys(String json, OperationOutcome oo) {
        JsonFactory factory = new JsonFactory();
        factory.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

        ObjectMapper mapper = new ObjectMapper(factory);

        try {
            // Esto lanzará JsonProcessingException si hay claves duplicadas
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            HapiFhirUtils.addErrorIssue("", e.getOriginalMessage(), oo);
            return null;
        } catch (IOException e) {
            HapiFhirUtils.addErrorIssue("", "Error al leer el JSON: " + e.getMessage(), oo);
            return null;
        }
    }
}
