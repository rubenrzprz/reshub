package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.security.RequestActor;
import com.rubenrzprz.reshub.security.RequestActorResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reservations", description = "Reservation read operations")
@RestController
public class ReservationController {

  private final ReservationQueryService reservationQueryService;
  private final RequestActorResolver actorResolver;

  public ReservationController(
    ReservationQueryService reservationQueryService,
    RequestActorResolver actorResolver
  ) {
    this.reservationQueryService = reservationQueryService;
    this.actorResolver = actorResolver;
  }

  @Operation(summary = "Get reservation by id")
  @ApiResponse(responseCode = "200", description = "Reservation found and authorized")
  @ApiResponse(responseCode = "401", description = "Missing/invalid actor context")
  @ApiResponse(responseCode = "403", description = "Forbidden by role scope")
  @ApiResponse(responseCode = "404", description = "Reservation not found")
  @GetMapping("/reservations/{id}")
  public ReservationView getById(
    @PathVariable UUID id,
    @RequestHeader(name = "X-User-Id", required = false) String userId,
    @RequestHeader(name = "X-Role", required = false) String role,
    @RequestHeader(name = "X-Hotel-Id", required = false) String hotelId,
    @RequestHeader(name = "X-Agency-Id", required = false) String agencyId
  ) {
    RequestActor actor = actorResolver.resolve(userId, role, hotelId, agencyId);
    return reservationQueryService.getById(id, actor);
  }
}
