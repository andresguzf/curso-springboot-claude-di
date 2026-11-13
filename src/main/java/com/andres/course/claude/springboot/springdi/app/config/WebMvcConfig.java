package com.andres.course.claude.springboot.springdi.app.config;

import com.andres.course.claude.springboot.springdi.app.interceptors.LoadingTimeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Configuración de Spring MVC: registra los interceptores
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoadingTimeInterceptor loadingTimeInterceptor;

    public WebMvcConfig(LoadingTimeInterceptor loadingTimeInterceptor) {
        this.loadingTimeInterceptor = loadingTimeInterceptor;
    }

    // "/**" aplica el interceptor a todos los endpoints, excepto los excluidos.
    // /api/app/date y /api/app/languages quedan fuera: no se miden, no se registran
    // en el log y su JSON no lleva "elapsedMs" (el advice solo lo agrega si el
    // interceptor midió el request).
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loadingTimeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/app/date", "/api/app/languages");
    }
}
