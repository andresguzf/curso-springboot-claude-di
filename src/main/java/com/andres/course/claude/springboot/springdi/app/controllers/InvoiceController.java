package com.andres.course.claude.springboot.springdi.app.controllers;

import com.andres.course.claude.springboot.springdi.app.models.Invoice;
import com.andres.course.claude.springboot.springdi.app.services.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    // Se inyecta la interfaz (el contrato), no la implementación
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;  
    }

    @GetMapping
    public Invoice show() {
        Invoice invoice = invoiceService.getInvoice();
        // Con @ApplicationScope llega el proxy: toString() se delega al objeto real
        // (misma instancia que en el constructor) y hashCode() es el del proxy
        log.info("show(): instancia = {} | hashCode = {}", invoice, invoice.hashCode());
        return invoice;
    }
}
