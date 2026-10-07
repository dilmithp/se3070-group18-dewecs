package com.group18.dewecs.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * Central error mapping. Browsers (requests that accept {@code text/html}) get the friendly
 * {@code templates/error.html} page with the right HTTP status; every other client, and every request under
 * {@code /api/} whatever it accepts, gets the RFC 7807 {@link ProblemDetail} JSON. Framework exceptions (unknown URL, wrong method, bad path
 * variable, missing parameter) keep their own 4xx status instead of being reported as 500.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return respond(request, HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler({WarningValidationException.class, ShelterValidationException.class,
            RescueRequestValidationException.class, ReliefValidationException.class,
            GroundReportValidationException.class, CitizenValidationException.class})
    public Object handleBusinessRule(RuntimeException ex, HttpServletRequest request) {
        return respond(request, HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("fieldErrors", fieldErrors);
        return respond(request, HttpStatus.BAD_REQUEST, "Validation failed", problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleBadPathVariable(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return respond(request, HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'.", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respond(request, HttpStatus.BAD_REQUEST, "The request body is missing or is not valid JSON.", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleUploadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return respond(request, HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file is too large.", null);
    }

    /** Unknown URL (404), wrong HTTP method (405), missing parameter (400) and other Spring MVC errors. */
    @ExceptionHandler({ServletException.class, ErrorResponseException.class, TypeMismatchException.class})
    public Object handleFramework(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            return respond(request, errorResponse.getStatusCode(), errorResponse.getBody().getDetail(), null);
        }
        if (ex instanceof TypeMismatchException) {
            return respond(request, HttpStatus.BAD_REQUEST, "The request contained an invalid value.", null);
        }
        return handleUnexpected(ex, request);
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(request, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", null);
    }

    private Object respond(HttpServletRequest request, HttpStatusCode status, String detail, ProblemDetail problem) {
        if (acceptsHtml(request) && !isApiRequest(request)) {
            HttpStatus resolved = HttpStatus.resolve(status.value());
            ModelAndView page = new ModelAndView("error");
            page.setStatus(status);
            page.addObject("status", status.value());
            page.addObject("error", resolved != null ? resolved.getReasonPhrase() : "Error");
            page.addObject("message", status.is5xxServerError()
                    ? "Something went wrong on our side. Please try again." : detail);
            return page;
        }
        ProblemDetail body = problem != null ? problem : ProblemDetail.forStatusAndDetail(status, detail);
        HttpStatus resolved = HttpStatus.resolve(status.value());
        body.setTitle(resolved != null ? resolved.getReasonPhrase() : "Error");
        // An explicit content type keeps this JSON even when the client's Accept header says text/html.
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    private boolean acceptsHtml(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }
}
