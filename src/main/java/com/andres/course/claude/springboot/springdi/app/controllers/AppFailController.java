package com.andres.course.claude.springboot.springdi.app.controllers;

import com.andres.course.claude.springboot.springdi.app.exceptions.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

// Controller para probar el manejo de errores: divide dos números que envía el
// usuario. Ya no tiene try-catch: la división por cero (ArithmeticException) la
// maneja GlobalExceptionHandler, el @RestControllerAdvice del proyecto.
@RestController
@RequestMapping("/api/fail")
public class AppFailController {

    // Cantidad de decimales del resultado
    private static final int DECIMALS = 4;

    // Productos de ejemplo para simular la búsqueda de un recurso por id
    private static final Map<Long, String> PRODUCTS = Map.of(
            1L, "Notebook",
            2L, "Mouse inalámbrico",
            3L, "Monitor 27\"");

    // GET /api/fail                      -> usa los valores por defecto: 10 / 1
    // GET /api/fail?dividend=10&divisor=3 -> 200 con el resultado (4 decimales)
    // GET /api/fail?dividend=10&divisor=0 -> ArithmeticException: la maneja
    //                                        GlobalExceptionHandler (400 con mensaje claro)
    @GetMapping
    public Map<String, Object> handler(@RequestParam(defaultValue = "10") BigDecimal dividend,
                                       @RequestParam(defaultValue = "1") BigDecimal divisor) {
        // divide() con escala 4 redondea a 4 decimales; si divisor es 0 lanza ArithmeticException
        BigDecimal result = dividend.divide(divisor, DECIMALS, RoundingMode.HALF_UP);

        return Map.of(
                "dividend", dividend,
                "divisor", divisor,
                "result", result);
    }

    // GET /api/fail/products/1 -> 200 con el producto
    // GET /api/fail/products/9 -> ResourceNotFoundException: la maneja
    //                             GlobalExceptionHandler (404 "Recurso no encontrado")
    @GetMapping("/products/{id}")
    public Map<String, Object> product(@PathVariable Long id) {
        String name = PRODUCTS.get(id);
        if (name == null) {
            throw new ResourceNotFoundException("el producto", id);
        }
        return Map.of("id", id, "name", name);
    }
}
