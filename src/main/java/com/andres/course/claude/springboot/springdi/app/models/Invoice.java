package com.andres.course.claude.springboot.springdi.app.models;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Invoice {

    // Inyección de dependencia mediante atributo (field injection)
    private final Customer customer;

    @Value("${invoice.description}")
    private String description;

    Invoice(Customer customer) {
        this.customer = customer;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
