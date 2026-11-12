package com.andres.course.claude.springboot.springdi.app.services;

import com.andres.course.claude.springboot.springdi.app.models.Invoice;
import org.springframework.stereotype.Service;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final Invoice invoice;

    // Inyección por constructor: con un solo constructor no hace falta @Autowired
    public InvoiceServiceImpl(Invoice invoice) {
        this.invoice = invoice;
    }

    @Override
    public Invoice getInvoice() {
        return invoice;
    }
}
