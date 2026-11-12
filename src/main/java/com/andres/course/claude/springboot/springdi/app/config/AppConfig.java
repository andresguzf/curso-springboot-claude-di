package com.andres.course.claude.springboot.springdi.app.config;

import com.andres.course.claude.springboot.springdi.app.models.InvoiceItem;
import com.andres.course.claude.springboot.springdi.app.models.Product;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class AppConfig {

    // Items de una compra de componentes de PC
    @Bean
    public List<InvoiceItem> itemsOffice() {
        Product notebook = new Product("Notebook", new BigDecimal("899990"));
        Product mouse = new Product("Mouse inalámbrico", new BigDecimal("19990"));
        Product monitor = new Product("Monitor 27\"", new BigDecimal("249990"));

        return List.of(
                new InvoiceItem(notebook, 1),
                new InvoiceItem(mouse, 2),
                new InvoiceItem(monitor, 2)
        );
    }

    // Items de una compra de supermercado
    @Bean
    public List<InvoiceItem> itemsSupermarket() {
        Product milk = new Product("Leche entera 1 L", new BigDecimal("1190"));
        Product bread = new Product("Pan marraqueta 1 kg", new BigDecimal("2490"));
        Product rice = new Product("Arroz grado 1 kg", new BigDecimal("1590"));
        Product eggs = new Product("Huevos (docena)", new BigDecimal("3990"));
        Product apples = new Product("Manzanas 1 kg", new BigDecimal("1790"));
        Product oil = new Product("Aceite vegetal 1 L", new BigDecimal("2990"));
        Product sugar = new Product("Azúcar 1 kg", new BigDecimal("1290"));

        return List.of(
                new InvoiceItem(milk, 6),
                new InvoiceItem(bread, 1),
                new InvoiceItem(rice, 2),
                new InvoiceItem(eggs, 1),
                new InvoiceItem(apples, 2),
                new InvoiceItem(oil, 1),
                new InvoiceItem(sugar, 1)
        );
    }
}
