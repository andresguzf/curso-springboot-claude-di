package com.andres.course.claude.springboot.springdi.app.controllers;

import com.andres.course.claude.springboot.springdi.app.models.Invoice;
import com.andres.course.claude.springboot.springdi.app.services.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
        // Con @SessionScope llega el proxy: toString() se delega al objeto real de la
        // sesión (igual en toda la sesión, distinto en otra) y hashCode() es el del
        // proxy (siempre igual)
        // toString() se llama aquí, antes de log.info: en la primera petición de cada
        // sesión esa llamada crea el objeto real, y si ocurriera dentro de log.info, Logback
        // descartaría los logs del constructor y de @PostConstruct (logs anidados)
        String instance = invoice.toString();
        log.info("show(): instancia = {} | hashCode = {}", instance, invoice.hashCode());
        return invoice;
    }

    // Cierra solo la factura de la sesión actual, sin invalidar la sesión (puede
    // tener otros datos). Spring destruye la factura y se ejecuta su @PreDestroy.
    // Es POST porque cambia el estado del servidor.
    @PostMapping("/close")
    public ResponseEntity<Void> close() {
        log.info("close(): cerrando la factura de la sesión");
        invoiceService.closeInvoice();
        return ResponseEntity.noContent().build();
    }
}
