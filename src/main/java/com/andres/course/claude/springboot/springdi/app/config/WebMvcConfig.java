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

    // "/**" aplica el interceptor a todos los endpoints
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loadingTimeInterceptor).addPathPatterns("/**");
    }
}
