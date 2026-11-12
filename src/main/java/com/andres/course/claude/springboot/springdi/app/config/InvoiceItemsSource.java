package com.andres.course.claude.springboot.springdi.app.config;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Calificador personalizado para elegir qué lista de items recibe la factura.
 * <p>
 * Se pone en el método {@code @Bean} que crea la lista y en el punto de
 * inyección (parámetro del constructor). Spring inyecta el bean cuyo
 * {@link Source} coincide. A diferencia de {@code @Qualifier("nombre")}, el
 * origen es un enum: un error de tipeo no compila.
 */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface InvoiceItemsSource {

    Source value();

    enum Source {
        OFFICE,
        SUPERMARKET,
        JSON
    }
}
