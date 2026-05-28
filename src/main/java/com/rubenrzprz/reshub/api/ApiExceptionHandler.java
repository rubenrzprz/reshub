package com.rubenrzprz.reshub.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "request validation failed");
    pd.setTitle("Bad Request");
    pd.setProperty("code", "validation_failed");
    pd.setProperty("path", request.getRequestURI());
    pd.setProperty("errors", validationErrors(ex));
    return pd;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "malformed request body");
    pd.setTitle("Bad Request");
    pd.setProperty("code", "invalid_request_body");
    pd.setProperty("path", request.getRequestURI());
    return pd;
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "invalid request parameter");
    pd.setTitle("Bad Request");
    pd.setProperty("code", "invalid_request_parameter");
    pd.setProperty("path", request.getRequestURI());
    pd.setProperty("parameter", ex.getName());
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

  private List<ValidationError> validationErrors(MethodArgumentNotValidException ex) {
    return ex.getBindingResult()
      .getFieldErrors()
      .stream()
      .map(this::validationError)
      .toList();
  }

  private ValidationError validationError(FieldError error) {
    return new ValidationError(error.getField(), error.getDefaultMessage());
  }

  private record ValidationError(String field, String message) {
  }
}
