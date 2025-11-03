package com.rubenrzprz.reshub.health;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
  name = "HealthResponse",
  description = "Minimal health snapshot of the API."
)
public record HealthResponse(
  @Schema(
    description = "Application identifier",
    example = "reshub"
  )
  String app,
  @Schema(
    description = "Overall status of the service",
    allowableValues = {"ok"},
    example = "ok"
  )
  String status
) {}
