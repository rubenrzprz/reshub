package com.rubenrzprz.reshub.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenRequest(
  @NotBlank
  @Email
  @Size(max = 254)
  String email,

  @NotBlank
  String password
) {
}
