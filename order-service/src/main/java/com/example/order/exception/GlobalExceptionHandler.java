package com.example.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Centralized error handling — Template Method pattern.
 *
 * Every handler follows the same shape:
 *   1. Create a ProblemDetail (RFC 7807 standard error format)
 *   2. Set status code, title, detail message, and type URI
 *   3. Return it — Spring serializes it to JSON automatically
 *
 * RFC 7807 response shape:
 * {
 *   "type": "https://api.example.com/errors/order-not-found",
 *   "title": "Order not found",
 *   "status": 404,
 *   "detail": "No order found with id: <uuid>"
 * }
 *
 * Spring Boot 3 ships ProblemDetail built-in — no extra library needed.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Order not found");
        pd.setType(URI.create("https://api.example.com/errors/order-not-found"));
        return pd;
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidStatusTransitionException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Invalid status transition");
        pd.setType(URI.create("https://api.example.com/errors/invalid-transition"));
        return pd;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setTitle("Insufficient stock");
        pd.setType(URI.create("https://api.example.com/errors/insufficient-stock"));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Validation failed");
        pd.setType(URI.create("https://api.example.com/errors/validation-failed"));
        return pd;
    }
}
