package com.andres.course.claude.springboot.springdi.app.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// Manejo centralizado de excepciones de la aplicación: cada método
// @ExceptionHandler atrapa un tipo de excepción y la convierte en una respuesta
// ProblemDetail, sin necesidad de try-catch en los controllers.
// ProblemDetail admite propiedades propias: todas las respuestas llevan
// "timestamp" (fecha y hora UTC del error) para seguimiento y trazabilidad.
// @Order(HIGHEST_PRECEDENCE): Spring Boot registra su propio advice de errores
// (spring.mvc.problemdetails.enabled=true) que maneja, entre otros,
// MethodArgumentTypeMismatchException. Este advice va primero para que sus
// @ExceptionHandler (por ejemplo, NumberFormatException) tengan prioridad; lo que
// no maneja aquí sigue cayendo en el de Spring Boot.
// Sin filtro (annotations/basePackages): una ruta que no existe no llega a ningún
// controller, y un advice filtrado por controller no la vería.
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Maneja los 404:
    // - ResourceNotFoundException: se busca un recurso por su id y no existe.
    // - NoResourceFoundException / NoHandlerFoundException: la ruta (página o
    //   endpoint) no existe. Spring Boot lanza NoResourceFoundException para las
    //   rutas que no coinciden con ningún controller ni archivo estático.
    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class, NoHandlerFoundException.class})
    public ProblemDetail handleNotFound(Exception ex, HttpServletRequest request) {
        ProblemDetail problem;
        if (ex instanceof ResourceNotFoundException) {
            problem = problem(HttpStatus.NOT_FOUND, ex.getMessage());
            problem.setTitle("Recurso no encontrado");
        } else {
            problem = problem(HttpStatus.NOT_FOUND,
                    "La ruta " + request.getRequestURI() + " no existe. Revisa que la dirección esté bien escrita.");
            problem.setTitle("Página no encontrada");
        }
        log.warn("handleNotFound(): {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem;
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

    // Maneja NumberFormatException: un texto que no se puede convertir a número.
    // Cuando un @RequestParam numérico recibe, por ejemplo, "2x", Spring lanza
    // MethodArgumentTypeMismatchException con NumberFormatException como causa;
    // @ExceptionHandler también busca por la causa, así que llega a este método.
    @ExceptionHandler(NumberFormatException.class)
    public ProblemDetail handleNumberFormat(NumberFormatException ex) {
        log.warn("handleNumberFormat(): valor no numérico: {}", ex.getMessage());
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST,
                "No pudimos completar la operación porque uno de los valores no es un número válido.");
        problem.setTitle("Valor no numérico");
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
