package com.andres.course.claude.springboot.springdi.app.exceptions;

// Se lanza cuando se busca un recurso por su id y no existe.
// GlobalExceptionHandler la convierte en una respuesta 404.
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("No encontramos " + resource + " con id " + id + ".");
    }
}
