package com.smarthealth.hms.web;

import com.smarthealth.hms.exception.BusinessRuleException;
import com.smarthealth.hms.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Safe error pages for the web UI: users never see stack traces or internal messages. */
@ControllerAdvice(basePackages = "com.smarthealth.hms.web")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ResourceNotFoundException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        return page(model, 404, "Not found", "The page or record you requested does not exist.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(Model model) {
        return page(model, 403, "Access denied", "You do not have permission to access this resource.");
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(Model model) {
        return page(model, 400, "Bad request", "A required parameter is missing or invalid.");
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String businessRule(BusinessRuleException ex, Model model) {
        return page(model, 400, "Request not allowed", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception ex, Model model) {
        log.error("Unexpected error while handling a web request", ex);
        return page(model, 500, "Something went wrong", "An unexpected error occurred. Please try again later.");
    }

    private String page(Model model, int status, String title, String message) {
        model.addAttribute("status", status);
        model.addAttribute("error", title);
        model.addAttribute("message", message);
        return "error";
    }
}
