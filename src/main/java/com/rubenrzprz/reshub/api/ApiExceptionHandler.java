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

  @ExceptionHandler(ApiProblemException.class)
  ProblemDetail handleApiProblem(ApiProblemException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
    pd.setTitle(titleFor(ex.status()));
    pd.setProperty("code", ex.code());
    pd.setProperty("path", request.getRequestURI());
    return pd;
  }

  @ExceptionHandler(ResponseStatusException.class)
  ProblemDetail handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
    HttpStatusCode code = ex.getStatusCode();
    String detail = ex.getReason() == null ? "request failed" : ex.getReason();
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(code, detail);
    pd.setTitle(titleFor(code));
    pd.setProperty("code", mapCode(code));
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

  private String mapCode(HttpStatusCode status) {
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
      return "conflict";
    }
    if (status.value() == 400) {
      return "bad_request";
    }
    return "request_failed";
  }
}
