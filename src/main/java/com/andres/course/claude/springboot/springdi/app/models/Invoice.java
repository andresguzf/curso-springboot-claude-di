package com.andres.course.claude.springboot.springdi.app.models;

import com.andres.course.claude.springboot.springdi.app.config.InvoiceItemsSource;
import com.andres.course.claude.springboot.springdi.app.config.InvoiceItemsSource.Source;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.util.List;

@Component
@SessionScope
// @SessionScope: se crea una instancia por sesión HTTP (cookie JSESSIONID). Todas las
// peticiones de un mismo navegador comparten la factura; otro navegador tiene la suya.
// Se destruye cuando la sesión termina (expira, se invalida o se detiene la aplicación).
// Como es un scope web, Spring inyecta un proxy CGLIB (Invoice$$SpringCGLIB$$0)
// que en cada petición delega en la instancia de la sesión en curso.
// Jackson intenta serializar también las propiedades internas del proxy:
// advisors y targetSource tienen referencias cíclicas (error al serializar) y
// el resto (advisorCount, frozen, targetObject...) ensucia el JSON. Se ignoran todas.
@JsonIgnoreProperties({"targetSource", "advisors", "advisorCount", "exposeProxy", "frozen",
        "preFiltered", "proxiedInterfaces", "proxyTargetClass", "targetClass", "targetObject"})
public class Invoice {

    private static final Logger log = LoggerFactory.getLogger(Invoice.class);

    // Inyección de dependencia por constructor (constructor injection)
    private final Customer customer;
    private final List<InvoiceItem> items;

    @Value("${invoice.description}")
    private String description;

    // Hay cuatro beans List<InvoiceItem> (itemsOffice, itemsSupermarket, itemsJson e itemsCsv):
    // el calificador personalizado @InvoiceItemsSource indica cuál inyectar
    Invoice(Customer customer, @InvoiceItemsSource(Source.CSV) List<InvoiceItem> items) {
        this.customer = customer;
        this.items = items;
        // Aquí description todavía es null: @Value se inyecta después del constructor
        log.info("Constructor de Invoice: description = {}", description);
        // Instancia creada por Spring: toString() por defecto muestra Clase@hashCode (en hexadecimal)
        log.info("Constructor de Invoice: instancia = {} | hashCode = {}", this, this.hashCode());
    }

    // Ciclo de vida: se ejecuta después del constructor y de inyectar todas las
    // dependencias (incluidos los @Value). Ya se puede usar todo.
    // Con @SessionScope se ejecuta una vez por sesión, en su primera petición.
    @PostConstruct
    public void init() {
        log.info("@PostConstruct: factura '{}' | items: {} | total: {}", description, items.size(), getTotal());
    }

    // Ciclo de vida: se ejecuta antes de destruir el bean. Sirve para liberar recursos.
    // Con @SessionScope se ejecuta cuando termina la sesión (expira, se invalida
    // o se detiene la aplicación) o cuando se cierra la factura con POST /api/invoices/close.
    @PreDestroy
    public void destroy() {
        log.info("@PreDestroy: el componente Invoice se va a destruir");
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Total de la factura: suma de los totales de cada item
    public BigDecimal getTotal() {
        // Con reduce: suma exacta en BigDecimal
        return items.stream()
                .map(InvoiceItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Alternativa con sum(): convierte cada total a double con mapToDouble
        // (puede perder precisión con montos decimales)
        // double total = items.stream()
        //         .mapToDouble(item -> item.getTotal().doubleValue())
        //         .sum();
        // return BigDecimal.valueOf(total);
    }
}
