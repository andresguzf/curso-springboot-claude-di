package com.andres.course.claude.springboot.springdi.app.services;

import com.andres.course.claude.springboot.springdi.app.models.Invoice;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

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

    // Con @SessionScope, la factura se guarda en la sesión con el nombre
    // "scopedTarget.invoice". Se quita solo ese atributo: el resto de la sesión se
    // mantiene. Al quitarlo, Spring ejecuta el @PreDestroy de la factura, y la
    // siguiente petición de la misma sesión crea una factura nueva.
    // Si no hay sesión, no hace nada (no crea una sesión nueva).
    @Override
    public void closeInvoice() {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        attributes.removeAttribute(ScopedProxyUtils.getTargetBeanName("invoice"), RequestAttributes.SCOPE_SESSION);
    }
}
