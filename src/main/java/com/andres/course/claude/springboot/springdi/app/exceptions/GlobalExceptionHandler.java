package com.andres.course.claude.springboot.springdi.app.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// Manejo centralizado de excepciones de los @RestController: cada método
// @ExceptionHandler atrapa un tipo de excepción y la convierte en una respuesta
// ProblemDetail, sin necesidad de try-catch en los controllers.
// ProblemDetail admite propiedades propias: todas las respuestas llevan
// "timestamp" (fecha y hora UTC del error) para seguimiento y trazabilidad.
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    // Maneja exclusivamente ArithmeticException (por ejemplo, una división por cero).
    // El detalle técnico va al log; al usuario se le muestra un mensaje simple.
    @ExceptionHandler({ArithmeticException.class})
    public ProblemDetail handleArithmetic(ArithmeticException ex) {
        log.warn("handleArithmetic(): error en la operación: {}", ex.getMessage());
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST,
                "No pudimos completar la operación porque no se puede dividir un número por cero.");
        problem.setTitle("Error en la operación");
        return problem;
    }

    // Crea el ProblemDetail con el status y el mensaje, y le agrega la fecha y hora
    // del error. Instant se serializa en ISO-8601 UTC, por ejemplo 2026-11-16T15:10:42.123Z.
    private static ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
