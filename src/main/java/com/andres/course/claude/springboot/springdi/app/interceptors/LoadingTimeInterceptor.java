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
    private static final String ELAPSED_MS = LoadingTimeInterceptor.class.getName() + ".elapsedMs";

    // Antes de ejecutar el controller: guarda el instante de inicio.
    // Devuelve true para que la petición continúe hacia el controller.
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.nanoTime());
        log.info("preHandle(): entrando a {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    // Al terminar la petición (incluso si hubo una excepción): registra el tiempo.
    // Si el JSON ya incluyó "elapsedMs", se registra exactamente ese mismo valor.
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long elapsedMs = elapsedMillis(request);
        if (elapsedMs != null) {
            log.info("afterCompletion(): {} {} -> status {} en {} ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
        }
    }

    // Milisegundos transcurridos desde preHandle(), o null si el request no pasó
    // por este interceptor. Se calcula una sola vez por request y se guarda: la
    // primera llamada fija el valor y las siguientes devuelven el mismo. Así el
    // "elapsedMs" del JSON (ElapsedTimeResponseAdvice, que lo pide primero) y el
    // log de afterCompletion() muestran exactamente el mismo número.
    // En respuestas sin body (204) el advice no lo pide y se calcula al terminar.
    public static Long elapsedMillis(HttpServletRequest request) {
        if (request.getAttribute(ELAPSED_MS) instanceof Long elapsed) {
            return elapsed;
        }
        if (request.getAttribute(START_TIME) instanceof Long start) {
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            request.setAttribute(ELAPSED_MS, elapsed);
            return elapsed;
        }
        return null;
    }
}
