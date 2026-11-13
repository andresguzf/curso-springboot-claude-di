package com.andres.course.claude.springboot.springdi.app.config;

import com.andres.course.claude.springboot.springdi.app.config.InvoiceItemsSource.Source;
import com.andres.course.claude.springboot.springdi.app.models.InvoiceItem;
import com.andres.course.claude.springboot.springdi.app.models.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.web.context.ContextCleanupListener;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class AppConfig {

    // Necesario solo para beans @ApplicationScope: se guardan en el ServletContext y
    // sus @PreDestroy los ejecuta ServletContextScope al cerrarse el ServletContext.
    // Spring Boot (Tomcat embebido) no registra por defecto el listener que lo
    // dispara: ContextCleanupListener lo hace al detener la aplicación.
    // Con @RequestScope o @SessionScope no hace falta: Spring ejecuta el @PreDestroy
    // al terminar la petición o la sesión.
    @Bean
    public ContextCleanupListener contextCleanupListener() {
        return new ContextCleanupListener();
    }

    // Aplica a cada sesión nueva el tiempo de expiración por inactividad en segundos
    // (app.session.timeout). Spring Boot registra automáticamente este listener.
    @Bean
    public HttpSessionListener sessionTimeoutListener(@Value("${app.session.timeout}") Duration timeout) {
        return new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent event) {
                event.getSession().setMaxInactiveInterval((int) timeout.toSeconds());
            }
        };
    }

    // Items de una compra de componentes de PC
    @Bean
    @InvoiceItemsSource(Source.OFFICE)
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
    @InvoiceItemsSource(Source.SUPERMARKET)
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

    // Items leídos desde src/main/resources/data/invoice-items.json.
    // Spring inyecta en los parámetros del método el JsonMapper (Jackson 3)
    // que configura Spring Boot y el archivo del classpath.
    @Bean
    @Primary
    @InvoiceItemsSource(Source.JSON)
    public List<InvoiceItem> itemsJson(JsonMapper jsonMapper,
                                       @Value("classpath:data/invoice-items.json") Resource itemsFile) {
        try (InputStream inputStream = itemsFile.getInputStream()) {
            return jsonMapper.readValue(inputStream, new TypeReference<List<InvoiceItem>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + itemsFile.getFilename(), e);
        }
    }

    // Items leídos desde src/main/resources/data/invoice-items.csv.
    // Formato: name,price,quantity,createdAt,updatedAt (la primera línea es el encabezado).
    @Bean
    @InvoiceItemsSource(Source.CSV)
    public List<InvoiceItem> itemsCsv(@Value("classpath:data/invoice-items.csv") Resource itemsFile) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(itemsFile.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)                          // salta el encabezado
                    .filter(line -> !line.isBlank())  // ignora líneas vacías
                    .map(this::toInvoiceItem)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + itemsFile.getFilename(), e);
        }
    }

    // Convierte una línea del CSV en un InvoiceItem
    private InvoiceItem toInvoiceItem(String line) {
        String[] columns = line.split(",");
        Product product = new Product(columns[0].trim(), new BigDecimal(columns[1].trim()));
        product.setCreatedAt(LocalDateTime.parse(columns[3].trim()));
        product.setUpdatedAt(LocalDateTime.parse(columns[4].trim()));
        return new InvoiceItem(product, Integer.parseInt(columns[2].trim()));
    }
}
