package com.andres.course.claude.springboot.springdi.app.services;

import com.andres.course.claude.springboot.springdi.app.models.Invoice;

public interface InvoiceService {

    Invoice getInvoice();

    // Cierra solo la factura de la sesión actual (no invalida la sesión)
    void closeInvoice();
}
