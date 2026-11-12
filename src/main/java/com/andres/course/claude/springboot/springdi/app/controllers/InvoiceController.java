package com.andres.course.claude.springboot.springdi.app.controllers;

import com.andres.course.claude.springboot.springdi.app.models.Invoice;
import com.andres.course.claude.springboot.springdi.app.services.InvoiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    // Se inyecta la interfaz (el contrato), no la implementación
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public Invoice show() {
        return invoiceService.getInvoice();
    }
}
