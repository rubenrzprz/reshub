package com.rubenrzprz.reshub.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  ProblemDetail handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
    HttpStatusCode code = ex.getStatusCode();
    String detail = ex.getReason() == null ? "request failed" : ex.getReason();
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(code, detail);
    pd.setTitle(titleFor(code));
    pd.setProperty("code", mapCode(code, detail));
    pd.setProperty("path", request.getRequestURI());
    return pd;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error");
    pd.setTitle("Internal Server Error");
    pd.setProperty("code", "internal_error");
    pd.setProperty("path", request.getRequestURI());
    return pd;
  }

  private String titleFor(HttpStatusCode code) {
    if (code.value() == 400) {
      return "Bad Request";
    }
    if (code.value() == 401) {
      return "Unauthorized";
    }
    if (code.value() == 403) {
      return "Forbidden";
    }
    if (code.value() == 404) {
      return "Not Found";
    }
    if (code.value() == 409) {
      return "Conflict";
    }
    if (code.value() == 500) {
      return "Internal Server Error";
    }
    return "Request Failed";
  }

  private String mapCode(HttpStatusCode status, String detail) {
    if (status.value() == 401) {
      return "unauthorized_actor_context";
    }
    if (status.value() == 403) {
      return "forbidden_scope";
    }
    if (status.value() == 404) {
      return "reservation_not_found";
    }
    if (status.value() == 409) {
      if (detail.contains("duplicate external reference")) {
        return "duplicate_external_ref";
      }
      if (detail.contains("invalid reservation status transition")) {
        return "invalid_status_transition";
      }
      return "conflict";
    }
    if (status.value() == 400) {
      if (detail.contains("invalid cursor")) {
        return "invalid_cursor";
      }
      if (detail.contains("limit must be between")) {
        return "invalid_limit";
      }
      if (detail.contains("missing required reservation fields")) {
        return "invalid_reservation_payload";
      }
      if (detail.contains("comment body is required")) {
        return "invalid_comment_payload";
      }
      if (detail.contains("invalid reservation payload")) {
        return "invalid_reservation_payload";
      }
      return "bad_request";
    }
    return "request_failed";
  }
}
