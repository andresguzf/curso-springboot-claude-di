package com.andres.course.claude.springboot.springdi.app.models;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Invoice {

    // Inyección de dependencia por constructor (constructor injection)
    private final Customer customer;
    private final List<InvoiceItem> items;

    @Value("${invoice.description}")
    private String description;

    // Hay tres beans List<InvoiceItem> (itemsOffice, itemsSupermarket e itemsJson):
    // @Qualifier indica cuál inyectar
    Invoice(Customer customer, List<InvoiceItem> items) {
        this.customer = customer;
        this.items = items;
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
