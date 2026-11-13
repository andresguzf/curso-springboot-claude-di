package com.andres.course.claude.springboot.springdi.app.models;

import com.andres.course.claude.springboot.springdi.app.config.InvoiceItemsSource;
import com.andres.course.claude.springboot.springdi.app.config.InvoiceItemsSource.Source;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
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
    }

    // Ciclo de vida: se ejecuta una vez, después del constructor y de inyectar
    // todas las dependencias (incluidos los @Value). Ya se puede usar todo.
    @PostConstruct
    public void init() {
        log.info("@PostConstruct: factura '{}' | items: {} | total: {}", description, items.size(), getTotal());
    }

    // Ciclo de vida: se ejecuta cuando el contexto de Spring se cierra
    // (al detener la aplicación), antes de destruir el bean. Sirve para liberar recursos.
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
