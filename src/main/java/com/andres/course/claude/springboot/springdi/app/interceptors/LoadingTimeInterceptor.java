package com.andres.course.claude.springboot.springdi.app.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Interceptor que mide cuánto tarda cada request y lo registra en el log.
// Es un singleton compartido por todas las peticiones: el tiempo de inicio se
// guarda en el propio request (atributo), nunca en un campo de la clase.
@Component
public class LoadingTimeInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoadingTimeInterceptor.class);

    private static final String START_TIME = LoadingTimeInterceptor.class.getName() + ".startTime";

    // Antes de ejecutar el controller: guarda el instante de inicio.
    // Devuelve true para que la petición continúe hacia el controller.
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.nanoTime());
        log.info("preHandle(): entrando a {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    // Al terminar la petición (incluso si hubo una excepción): calcula el tiempo transcurrido.
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long elapsedMs = elapsedMillis(request);
        if (elapsedMs != null) {
            log.info("afterCompletion(): {} {} -> status {} en {} ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
        }
    }

    // Milisegundos transcurridos desde preHandle() hasta ahora, o null si el
    // request no pasó por este interceptor. Lo usa también ElapsedTimeResponseAdvice.
    public static Long elapsedMillis(HttpServletRequest request) {
        if (request.getAttribute(START_TIME) instanceof Long start) {
            return (System.nanoTime() - start) / 1_000_000;
        }
        return null;
    }
}
