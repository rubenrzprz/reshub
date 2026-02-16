package com.rubenrzprz.reshub.api;

import org.springframework.http.HttpStatusCode;

public class ApiProblemException extends RuntimeException {

  private final HttpStatusCode status;
  private final String code;

  public ApiProblemException(HttpStatusCode status, String code, String detail) {
    super(detail);
    this.status = status;
    this.code = code;
  }

  public HttpStatusCode status() {
    return status;
  }

  public String code() {
    return code;
  }
}
