package com.andres.course.claude.springboot.springdi.app.interceptors;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

// Agrega al JSON de respuesta el tiempo transcurrido en el request ("elapsedMs").
// ResponseBodyAdvice se ejecuta después del controller y antes de escribir el body,
// así que puede modificar la respuesta (un HandlerInterceptor no puede: cuando
// corre postHandle() el JSON de un @RestController ya se escribió).
// Se limita a los controllers del proyecto para no alterar el JSON de springdoc
// (/v3/api-docs) ni el de los errores de Spring Boot (/error).
@RestControllerAdvice(basePackages = "com.andres.course.claude.springboot.springdi.app.controllers")
public class ElapsedTimeResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final String ELAPSED_FIELD = "elapsedMs";

    private final JsonMapper jsonMapper;

    public ElapsedTimeResponseAdvice(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // Sin body (por ejemplo 204 No Content) o si no es JSON, no se toca la respuesta
        if (body == null || !MediaType.APPLICATION_JSON.isCompatibleWith(selectedContentType)
                || !(request instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }
        Long elapsedMs = LoadingTimeInterceptor.elapsedMillis(servletRequest.getServletRequest());
        if (elapsedMs == null) {
            return body;
        }
        // Errores con ProblemDetail: se agrega como una propiedad más
        if (body instanceof ProblemDetail problem) {
            problem.setProperty(ELAPSED_FIELD, elapsedMs);
            return problem;
        }
        // Objetos JSON: se agrega el campo al final, sin cambiar el resto
        JsonNode json = jsonMapper.valueToTree(body);
        if (json instanceof ObjectNode object) {
            object.put(ELAPSED_FIELD, elapsedMs);
            return object;
        }
        // Listas u otros valores: se envuelven en { "data": ..., "elapsedMs": ... }
        ObjectNode wrapper = jsonMapper.createObjectNode();
        wrapper.set("data", json);
        wrapper.put(ELAPSED_FIELD, elapsedMs);
        return wrapper;
    }
}
