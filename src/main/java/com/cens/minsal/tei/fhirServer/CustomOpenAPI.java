package com.cens.minsal.tei.fhirServer;

import ca.uhn.fhir.rest.openapi.OpenApiInterceptor;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import java.util.Collections;

public class CustomOpenAPI extends OpenApiInterceptor {

    @Override
    protected OpenAPI generateOpenApi(ServletRequestDetails theRequestDetails) {
      OpenAPI openApi = super.generateOpenApi(theRequestDetails);
      openApi.getComponents().addSecuritySchemes("x-api-key", oauth2ImplicitSecurityScheme());
      SecurityRequirement securityRequirement = new SecurityRequirement();
      securityRequirement.addList("x-api-key");
      openApi.security(Collections.singletonList(securityRequirement));
      return openApi;
    }

  private SecurityScheme oauth2ImplicitSecurityScheme() {
      SecurityScheme scheme = new SecurityScheme();
      scheme.type(Type.APIKEY)
          .in(In.HEADER)
          .scheme("x-api-key")
          .name("x-api-key");
      return scheme;
  }

}
