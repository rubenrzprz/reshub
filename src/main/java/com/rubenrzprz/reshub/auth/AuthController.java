package com.rubenrzprz.reshub.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Authentication and token issuance")
@RestController
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(summary = "Issue JWT token")
  @ApiResponse(responseCode = "200", description = "Token issued")
  @ApiResponse(responseCode = "400", description = "Invalid auth payload")
  @ApiResponse(responseCode = "401", description = "Invalid credentials")
  @PostMapping("/auth/token")
  public TokenResponse token(@RequestBody TokenRequest request) {
    return authService.issueToken(request);
  }
}
