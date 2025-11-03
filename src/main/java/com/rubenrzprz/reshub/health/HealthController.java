package com.rubenrzprz.reshub.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "Liveness & smoke checks")
@RestController
public class HealthController {

  @Value("${spring.application.name}")
  String appName;

  @Operation(
    summary = "Simple health check",
    description = "Returns a minimal OK status for smoke testing the app."
  )
  @ApiResponse(
    responseCode = "200",
    description = "Service is healthy",
    content = @Content(
      mediaType = "application/json",
      schema = @Schema(implementation = HealthResponse.class),
      examples = @ExampleObject(
        name = "healthy",
        value = "{\"app\":\"reshub\",\"status\":\"ok\"}"
      )
    )
  )
  @GetMapping("/health")
  public HealthResponse health() {
    return new HealthResponse(appName, "ok");
  }
}
