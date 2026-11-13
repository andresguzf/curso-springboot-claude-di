package com.andres.course.claude.springboot.springdi.app.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Controller de ejemplo con tres handlers simples. Sirve para ver el interceptor
// de tiempo en endpoints distintos y cómo ElapsedTimeResponseAdvice agrega
// "elapsedMs" a un objeto JSON y envuelve una lista en { "data": ..., "elapsedMs": ... }.
@RestController
@RequestMapping("/api/app")
public class AppController {

    // GET /api/app/greeting?name=Andrés -> saludo (name es opcional)
    @GetMapping("/greeting")
    public Map<String, String> greeting(@RequestParam(defaultValue = "mundo") String name) {
        return Map.of("message", "Hola, " + name + "!");
    }

    // GET /api/app/date -> fecha y fecha-hora actuales del servidor
    @GetMapping("/date")
    public Map<String, Object> date() {
        return Map.of(
                "date", LocalDate.now(),
                "dateTime", LocalDateTime.now()
        );
    }

    // GET /api/app/languages -> lista de lenguajes de programación
    @GetMapping("/languages")
    public List<String> languages() {
        return List.of("Java", "Kotlin", "JavaScript", "TypeScript", "Python", "Go");
    }
}
