package com.rubenrzprz.reshub.reservation;

import com.rubenrzprz.reshub.security.RequestActor;
import com.rubenrzprz.reshub.security.RequestActorResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Reservations", description = "Reservation read operations")
@RestController
public class ReservationController {

  private final ReservationQueryService reservationQueryService;
  private final ReservationCommandService reservationCommandService;
  private final RequestActorResolver actorResolver;

  public ReservationController(
    ReservationQueryService reservationQueryService,
    ReservationCommandService reservationCommandService,
    RequestActorResolver actorResolver
  ) {
    this.reservationQueryService = reservationQueryService;
    this.reservationCommandService = reservationCommandService;
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

  @Operation(summary = "List reservations with keyset pagination")
  @ApiResponse(responseCode = "200", description = "Reservations listed and authorized")
  @ApiResponse(responseCode = "400", description = "Invalid pagination/filter input")
  @ApiResponse(responseCode = "401", description = "Missing/invalid actor context")
  @GetMapping("/reservations")
  public ReservationListResponse list(
    @RequestParam(name = "limit", defaultValue = "50") int limit,
    @RequestParam(name = "cursor", required = false) String cursor,
    @RequestParam(name = "status", required = false) String status,
    @RequestHeader(name = "X-User-Id", required = false) String userId,
    @RequestHeader(name = "X-Role", required = false) String role,
    @RequestHeader(name = "X-Hotel-Id", required = false) String hotelId,
    @RequestHeader(name = "X-Agency-Id", required = false) String agencyId
  ) {
    if (limit < 1 || limit > 200) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 200");
    }
    RequestActor actor = actorResolver.resolve(userId, role, hotelId, agencyId);
    return reservationQueryService.list(actor, limit, cursor, status);
  }

  @Operation(summary = "Update reservation notes")
  @ApiResponse(responseCode = "200", description = "Reservation updated and authorized")
  @ApiResponse(responseCode = "401", description = "Missing/invalid actor context")
  @ApiResponse(responseCode = "403", description = "Forbidden by role scope")
  @ApiResponse(responseCode = "404", description = "Reservation not found")
  @PatchMapping("/reservations/{id}/notes")
  public ReservationView updateNotes(
    @PathVariable UUID id,
    @RequestBody ReservationNotesUpdateRequest request,
    @RequestHeader(name = "X-User-Id", required = false) String userId,
    @RequestHeader(name = "X-Role", required = false) String role,
    @RequestHeader(name = "X-Hotel-Id", required = false) String hotelId,
    @RequestHeader(name = "X-Agency-Id", required = false) String agencyId
  ) {
    RequestActor actor = actorResolver.resolve(userId, role, hotelId, agencyId);
    return reservationCommandService.updateNotes(id, request.notes(), actor);
  }

  @Operation(summary = "Create internal reservation comment")
  @ApiResponse(responseCode = "201", description = "Comment created and authorized")
  @ApiResponse(responseCode = "400", description = "Invalid comment payload")
  @ApiResponse(responseCode = "401", description = "Missing/invalid actor context")
  @ApiResponse(responseCode = "403", description = "Forbidden by role scope/policy")
  @ApiResponse(responseCode = "404", description = "Reservation not found")
  @PostMapping("/reservations/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public ReservationCommentView addComment(
    @PathVariable UUID id,
    @RequestBody ReservationCommentCreateRequest request,
    @RequestHeader(name = "X-User-Id", required = false) String userId,
    @RequestHeader(name = "X-Role", required = false) String role,
    @RequestHeader(name = "X-Hotel-Id", required = false) String hotelId,
    @RequestHeader(name = "X-Agency-Id", required = false) String agencyId
  ) {
    RequestActor actor = actorResolver.resolve(userId, role, hotelId, agencyId);
    return reservationCommandService.addComment(id, request.body(), actor);
  }

  @Operation(summary = "Confirm reservation")
  @ApiResponse(responseCode = "200", description = "Reservation confirmed and authorized")
  @ApiResponse(responseCode = "401", description = "Missing/invalid actor context")
  @ApiResponse(responseCode = "403", description = "Forbidden by role scope")
  @ApiResponse(responseCode = "404", description = "Reservation not found")
  @ApiResponse(responseCode = "409", description = "Invalid status transition")
  @PostMapping("/reservations/{id}/confirm")
  public ReservationView confirm(
    @PathVariable UUID id,
    @RequestHeader(name = "X-User-Id", required = false) String userId,
    @RequestHeader(name = "X-Role", required = false) String role,
    @RequestHeader(name = "X-Hotel-Id", required = false) String hotelId,
    @RequestHeader(name = "X-Agency-Id", required = false) String agencyId
  ) {
    RequestActor actor = actorResolver.resolve(userId, role, hotelId, agencyId);
    return reservationCommandService.confirm(id, actor);
  }

  @Operation(summary = "Cancel reservation")
  @ApiResponse(responseCode = "200", description = "Reservation cancelled and authorized")
  @ApiResponse(responseCode = "401", description = "Missing/invalid actor context")
  @ApiResponse(responseCode = "403", description = "Forbidden by role scope")
  @ApiResponse(responseCode = "404", description = "Reservation not found")
  @ApiResponse(responseCode = "409", description = "Invalid status transition")
  @PostMapping("/reservations/{id}/cancel")
  public ReservationView cancel(
    @PathVariable UUID id,
    @RequestBody(required = false) ReservationCancelRequest request,
    @RequestHeader(name = "X-User-Id", required = false) String userId,
    @RequestHeader(name = "X-Role", required = false) String role,
    @RequestHeader(name = "X-Hotel-Id", required = false) String hotelId,
    @RequestHeader(name = "X-Agency-Id", required = false) String agencyId
  ) {
    RequestActor actor = actorResolver.resolve(userId, role, hotelId, agencyId);
    return reservationCommandService.cancel(id, request == null ? null : request.reason(), actor);
  }

  @Operation(summary = "Mark reservation as no-show")
  @ApiResponse(responseCode = "200", description = "Reservation marked no-show and authorized")
  @ApiResponse(responseCode = "401", description = "Missing/invalid actor context")
  @ApiResponse(responseCode = "403", description = "Forbidden by role scope")
  @ApiResponse(responseCode = "404", description = "Reservation not found")
  @ApiResponse(responseCode = "409", description = "Invalid status transition")
  @PostMapping("/reservations/{id}/noshow")
  public ReservationView noShow(
    @PathVariable UUID id,
    @RequestHeader(name = "X-User-Id", required = false) String userId,
    @RequestHeader(name = "X-Role", required = false) String role,
    @RequestHeader(name = "X-Hotel-Id", required = false) String hotelId,
    @RequestHeader(name = "X-Agency-Id", required = false) String agencyId
  ) {
    RequestActor actor = actorResolver.resolve(userId, role, hotelId, agencyId);
    return reservationCommandService.noShow(id, actor);
  }
}
