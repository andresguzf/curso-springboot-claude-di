# 2-spring-di

Proyecto del curso de **Spring Boot con Claude Code**. Incluye la skill
`spring-boot-best-practices`, que permite a Claude Code generar y mantener
proyectos Spring Boot con una arquitectura en capas y convenciones comunes para
todo el curso.

> **Estado actual:** estructura base generada con Spring Initializr (Spring
> Boot 4.1.2, Java 25) y un ejemplo de **inyección de dependencias** con los
> componentes `Invoice` y `Customer`, items de factura con sus productos, un
> service y un controller que expone `GET /api/invoices`.

## Contenido

- [Stack](#stack)
- [Requisitos](#requisitos)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Estructura del proyecto Spring Boot](#estructura-del-proyecto-spring-boot)
- [Ejemplo de inyección de dependencias](#ejemplo-de-inyección-de-dependencias)
- [Ciclo de vida del componente Invoice](#ciclo-de-vida-del-componente-invoice)
- [Alcance por sesión: @SessionScope](#alcance-por-sesión-sessionscope)
- [Interceptor: tiempo de cada request](#interceptor-tiempo-de-cada-request)
- [Controller de ejemplo: AppController](#controller-de-ejemplo-appcontroller)
- [Uso](#uso)
- [Arquitectura de capas](#arquitectura-de-capas)
- [Persistencia bajo demanda](#persistencia-bajo-demanda)
- [Vistas web (monolito)](#vistas-web-monolito)
- [Ejecutar la aplicación](#ejecutar-la-aplicación)
- [Endpoints útiles](#endpoints-útiles)
- [Pruebas de la skill](#pruebas-de-la-skill)

## Stack

| | |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1.2 (la skill usa la última versión estable ≥ 4.1.2 de start.spring.io) |
| Build | Maven con Maven Wrapper (`./mvnw`) |
| Packaging | jar |
| Configuración | `application.properties` |
| Dependencias base | Spring Web MVC, Validation, springdoc OpenAPI UI, DevTools, Actuator |
| Bajo demanda | Spring Data JPA + Hibernate + PostgreSQL, Thymeleaf + Tailwind CSS |

Coordenadas Maven:

- **groupId:** `com.andres.course.claude.springboot`
- **artifactId:** nombre del directorio del proyecto (`2-spring-di`)
- **package base:** `com.andres.course.claude.springboot.{artifactId}.app`

Como un package Java no admite guiones ni empezar con un número, el artifactId
se normaliza: `2-spring-di` → `com.andres.course.claude.springboot.springdi.app`.

## Requisitos

- [Claude Code](https://claude.com/claude-code)
- JDK 25
- `curl`, `unzip` y `python3` (los usa el script que genera el proyecto)
- Conexión a internet para descargar el proyecto desde https://start.spring.io
- PostgreSQL, solo si el proyecto usa persistencia (puede ser con Docker)

No hace falta tener Maven instalado: el proyecto incluye el Maven Wrapper.

## Estructura del repositorio

```
2-spring-di/
├── CLAUDE.md                                  # contexto del proyecto, skills y reglas de activación
├── README.md
├── .gitignore
├── .gitattributes
├── pom.xml                                    # proyecto Maven generado por Spring Initializr
├── mvnw, mvnw.cmd                             # Maven Wrapper
├── .mvn/wrapper/maven-wrapper.properties
├── src/                                       # código del proyecto (ver sección siguiente)
└── .claude/skills/
    ├── spring-boot-best-practices/
    │   ├── SKILL.md                           # instrucciones de la skill
    │   ├── scripts/create_project.sh          # genera el proyecto con Spring Initializr
    │   ├── references/
    │   │   ├── layers.md                      # plantillas: entity, DTO, mapper, repository, service, controller
    │   │   ├── configuration.md               # application.properties y PostgreSQL
    │   │   └── thymeleaf.md                   # layout, fragmentos y vistas con Tailwind
    │   └── evals/evals.json                   # casos de prueba de la skill
    └── spring-boot-best-practices-workspace/  # resultados de las pruebas (excluido en .gitignore)
```

Las carpetas `evals/` y `spring-boot-best-practices-workspace/` existen solo en
local: el `.gitignore` las excluye del repositorio.

## Estructura del proyecto Spring Boot

```
src/
├── main/
│   ├── java/com/andres/course/claude/springboot/springdi/app/
│   │   ├── Application.java                  # clase principal (@SpringBootApplication)
│   │   ├── config/
│   │   │   ├── AppConfig.java                # @Bean ContextCleanupListener + listas de items (PC, supermercado, JSON y CSV)
│   │   │   ├── InvoiceItemsSource.java       # calificador personalizado para elegir la lista
│   │   │   └── WebMvcConfig.java             # registra el interceptor (todos los endpoints salvo date y languages)
│   │   ├── interceptors/
│   │   │   ├── LoadingTimeInterceptor.java   # mide el tiempo de cada request y lo registra en el log
│   │   │   └── ElapsedTimeResponseAdvice.java # agrega "elapsedMs" al JSON de respuesta
│   │   ├── models/
│   │   │   ├── Customer.java                 # componente: nombre y apellido del cliente
│   │   │   ├── Invoice.java                  # componente @SessionScope: descripción + Customer e items + total + ciclo de vida
│   │   │   ├── InvoiceItem.java              # item: producto, cantidad y total
│   │   │   └── Product.java                  # producto: nombre, precio y fechas
│   │   ├── dto/package-info.java             # DTOs como record (vacío por ahora)
│   │   ├── mappers/package-info.java         # conversión entity <-> dto (vacío por ahora)
│   │   ├── repositories/package-info.java    # acceso a datos (vacío por ahora)
│   │   ├── services/
│   │   │   ├── InvoiceService.java           # contrato del service
│   │   │   └── InvoiceServiceImpl.java       # implementación (@Service)
│   │   ├── controllers/
│   │   │   ├── AppController.java            # GET /api/app/greeting, /date y /languages
│   │   │   └── InvoiceController.java        # GET /api/invoices y POST /api/invoices/close
│   │   └── exceptions/
│   │       ├── ResourceNotFoundException.java
│   │       └── GlobalExceptionHandler.java   # ProblemDetail: 404 y 400 de validación
│   └── resources/
│       ├── application.properties            # Swagger UI, Actuator, ProblemDetail, sesiones y datos de ejemplo
│       └── data/
│           ├── invoice-items.json            # 8 items de la factura en formato JSON
│           └── invoice-items.csv             # 10 items de la factura en formato CSV
└── test/java/com/andres/course/claude/springboot/springdi/app/
    └── ApplicationTests.java                 # verifica que el contexto arranca
```

- Los packages `dto`, `mappers` y `repositories` solo tienen un
  `package-info.java` que describe su propósito; se llenarán en las siguientes
  etapas del curso.
- La clase principal se llama `Application` y no `SpringDiApplication`, porque
  Initializr no genera un nombre de clase a partir de un artifactId que empieza
  con número.
- Dependencias actuales: `spring-boot-starter-webmvc`, `spring-boot-starter-validation`,
  `springdoc-openapi-starter-webmvc-ui`, `spring-boot-devtools` y
  `spring-boot-starter-actuator`. Sin JPA ni PostgreSQL.

## Ejemplo de inyección de dependencias

El ejemplo muestra cómo Spring crea los beans y los inyecta unos en otros:

```
Customer (@Component) ─────────────────────┐
                                           ├──▶ Invoice ──▶ InvoiceServiceImpl ──▶ InvoiceController ──▶ GET /api/invoices
itemsCsv (@Bean, @InvoiceItemsSource) ─────┘   (@Component)  (@Service)             (@RestController)
   ▲ lee data/invoice-items.csv
itemsOffice, itemsSupermarket (@Bean, no se usan por ahora)
itemsJson (@Bean, @Primary: se usa donde no hay calificador)
```

| Clase | Rol | Recibe por constructor |
|---|---|---|
| `Customer` | componente con `name` y `lastname` | — (valores con `@Value`) |
| `Invoice` | componente con `description` y `getTotal()` | `Customer` y `List<InvoiceItem>` (con `@InvoiceItemsSource`) |
| `AppConfig` | `@Configuration` con cuatro métodos `@Bean`: `itemsOffice`, `itemsSupermarket`, `itemsJson` e `itemsCsv` | — |
| `InvoiceItem` | item de la factura: `product`, `quantity` y `getTotal()` (POJO, no es bean) | — |
| `Product` | `name`, `price` (`BigDecimal`), `createdAt` y `updatedAt` (POJO, no es bean) | — |
| `InvoiceService` / `InvoiceServiceImpl` | lógica de negocio, desacoplada del controller | `Invoice` |
| `InvoiceController` | expone el endpoint REST | `InvoiceService` (la interfaz) |

Toda la inyección es **por constructor**, con campos `final` y sin
`@Autowired`: cuando una clase tiene un solo constructor, Spring lo usa
automáticamente. Así las dependencias son inmutables, quedan visibles en un
solo lugar y la clase se puede probar sin levantar Spring.

`Product` e `InvoiceItem` no son `@Component` porque son objetos de datos con
varias instancias distintas (un `@Component` es una sola instancia por clase).
Por eso las listas se arman en `AppConfig` con métodos `@Bean`:

| Bean | Productos |
|---|---|
| `itemsOffice` | 3 componentes de PC: notebook, mouse inalámbrico y monitor |
| `itemsSupermarket` | 7 productos de supermercado: leche, pan, arroz, huevos, manzanas, aceite y azúcar |
| `itemsJson` | 8 productos de supermercado leídos de `data/invoice-items.json` |
| `itemsCsv` | 10 productos de supermercado leídos de `data/invoice-items.csv` |

Como hay cuatro beans del mismo tipo `List<InvoiceItem>`, Spring no sabe cuál
inyectar y fallaría al arrancar. Para elegir se usa un **calificador
personalizado**.

### Calificador personalizado `@InvoiceItemsSource`

En lugar de `@Qualifier("itemsJson")`, el proyecto define su propia anotación
en `config/InvoiceItemsSource.java`. Se llama así (origen de los items de la
factura) para no confundirse con la clase `InvoiceItem`:

```java
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface InvoiceItemsSource {

    Source value();

    enum Source { OFFICE, SUPERMARKET, JSON, CSV }
}
```

- `@Qualifier` como meta-anotación la convierte en un calificador de Spring.
- `@Retention(RUNTIME)` permite que Spring la lea con la aplicación en ejecución.
- El valor es un enum: un error de tipeo (`Source.JSNO`) no compila, mientras
  que con `@Qualifier("itemsJsno")` el error solo aparece al arrancar.

Cada método `@Bean` de `AppConfig` se marca con su origen:

```java
@Bean
@InvoiceItemsSource(Source.OFFICE)
public List<InvoiceItem> itemsOffice() { ... }

@Bean
@InvoiceItemsSource(Source.SUPERMARKET)
public List<InvoiceItem> itemsSupermarket() { ... }

@Bean
@Primary
@InvoiceItemsSource(Source.JSON)
public List<InvoiceItem> itemsJson(...) { ... }

@Bean
@InvoiceItemsSource(Source.CSV)
public List<InvoiceItem> itemsCsv(...) { ... }
```

Y `Invoice` pide la lista que necesita en su constructor:

```java
Invoice(Customer customer, @InvoiceItemsSource(Source.CSV) List<InvoiceItem> items) {
    this.customer = customer;
    this.items = items;
}
```

Para usar otra lista, cambia el valor a `Source.OFFICE`, `Source.SUPERMARKET` o
`Source.JSON`.

`itemsJson` además es `@Primary`: es la lista que se inyecta cuando se pide una
`List<InvoiceItem>` sin ningún calificador. Cuando hay un calificador, como en
`Invoice`, el calificador manda sobre `@Primary`.

### Items desde un archivo JSON

Los 8 items de `itemsJson` están en `src/main/resources/data/invoice-items.json`.
Cada item trae el producto completo y la cantidad:

```json
[
  {
    "product": { "name": "Café molido 250 g", "price": 4990, "createdAt": "2026-11-01T09:00:00", "updatedAt": "2026-11-01T09:00:00" },
    "quantity": 2
  },
  ...
]
```

El método `@Bean` recibe por parámetro el `JsonMapper` de Jackson 3 que ya
configura Spring Boot y el archivo del classpath, y lo convierte en una
`List<InvoiceItem>`:

```java
@Bean
public List<InvoiceItem> itemsJson(JsonMapper jsonMapper,
                                   @Value("classpath:data/invoice-items.json") Resource itemsFile) {
    try (InputStream inputStream = itemsFile.getInputStream()) {
        return jsonMapper.readValue(inputStream, new TypeReference<List<InvoiceItem>>() {
        });
    } catch (IOException e) {
        throw new UncheckedIOException("No se pudo leer " + itemsFile.getFilename(), e);
    }
}
```

- `TypeReference` le indica a Jackson el tipo genérico completo (`List<InvoiceItem>`).
- Las fechas en formato ISO (`2026-11-01T09:00:00`) se convierten a `LocalDateTime`.
- Si el archivo no existe o tiene un error, la aplicación no arranca y el
  mensaje indica qué archivo falló.

Para cambiar los productos basta editar el JSON y reiniciar la aplicación; no
hay que tocar código.

### Items desde un archivo CSV

Los 10 items de `itemsCsv` están en `src/main/resources/data/invoice-items.csv`.
La primera línea es el encabezado y cada línea siguiente es un item:

```csv
name,price,quantity,createdAt,updatedAt
Lentejas 1 kg,2290,2,2026-11-02T10:00:00,2026-11-02T10:00:00
Atún en lata 160 g,1890,4,2026-11-02T10:05:00,2026-11-04T08:30:00
...
```

El método `@Bean` lee el archivo línea por línea, sin librerías externas:

```java
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
```

- Se lee en UTF-8 para respetar acentos como "Atún" o "Té".
- Cada línea se separa con `split(",")`, así que los nombres de producto no
  pueden contener comas. Si hiciera falta, se puede usar una librería como
  OpenCSV o Jackson CSV.

### Total de la factura

Los totales se calculan a partir de los items, no se guardan:

```java
// InvoiceItem: precio del producto por la cantidad
public BigDecimal getTotal() {
    return product.getPrice().multiply(BigDecimal.valueOf(quantity));
}

// Invoice: suma de los totales de cada item con reduce
public BigDecimal getTotal() {
    return items.stream()
            .map(InvoiceItem::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

`reduce` parte de `BigDecimal.ZERO` y va sumando cada total con
`BigDecimal::add`, así la suma es exacta. En el código queda comentada una
alternativa con `mapToDouble(...).sum()`: `sum()` solo existe en los streams
numéricos (`DoubleStream`, `IntStream`, `LongStream`), por lo que obliga a
pasar por `double` y puede perder precisión con montos decimales.

Como son getters, Jackson los incluye en el JSON como `total`, tanto en cada
item como en la factura. Se usa `BigDecimal` para no perder precisión con
montos de dinero.

Los valores del cliente y la descripción vienen de `application.properties`:

```properties
customer.name=John
customer.lastname=Doe
invoice.description=Compras de supermercado
```

Respuesta de `GET /api/invoices`:

```json
{
  "customer": { "lastname": "Doe", "name": "John" },
  "description": "Compras de supermercado",
  "items": [
    {
      "product": {
        "name": "Lentejas 1 kg",
        "price": 2290,
        "createdAt": "2026-11-02T10:00:00",
        "updatedAt": "2026-11-02T10:00:00"
      },
      "quantity": 2,
      "total": 4580
    },
    { "product": { "name": "Atún en lata 160 g", "price": 1890, "...": "..." }, "quantity": 4, "total": 7560 },
    "... (10 items en total)"
  ],
  "total": 37710,
  "elapsedMs": 28
}
```

El campo `elapsedMs` lo agrega `ElapsedTimeResponseAdvice` (ver
[Interceptor: tiempo de cada request](#interceptor-tiempo-de-cada-request)).

En esta etapa el service devuelve el componente `Invoice` directamente, por eso
la respuesta incluye las fechas `createdAt`/`updatedAt` del producto. Los DTOs
(que las ocultarán), el mapper y los repositorios se incorporarán más adelante.

### Ciclo de vida del componente `Invoice`

`Invoice` implementa los métodos de ciclo de vida con las anotaciones de
`jakarta.annotation` (ya incluidas por los starters de Spring Boot):

```java
Invoice(Customer customer, @InvoiceItemsSource(Source.CSV) List<InvoiceItem> items) {
    this.customer = customer;
    this.items = items;
    // Aquí description todavía es null: @Value se inyecta después del constructor
    log.info("Constructor de Invoice: description = {}", description);
    // Instancia creada por Spring: toString() por defecto muestra Clase@hashCode (en hexadecimal)
    log.info("Constructor de Invoice: instancia = {} | hashCode = {}", this, this.hashCode());
}

@PostConstruct
public void init() {
    log.info("@PostConstruct: factura '{}' | items: {} | total: {}", description, items.size(), getTotal());
}

@PreDestroy
public void destroy() {
    log.info("@PreDestroy: el componente Invoice se va a destruir");
}
```

| Momento | Qué pasa |
|---|---|
| Constructor | Spring crea el bean e inyecta las dependencias del constructor. Los campos con `@Value` todavía no tienen valor. |
| `@PostConstruct` | Se ejecuta una vez por instancia, cuando ya están inyectadas todas las dependencias y los `@Value`. Es el lugar para inicializar usando esos valores. |
| `@PreDestroy` | Se ejecuta antes de destruir el bean. Sirve para liberar recursos. Con un singleton es al detener la aplicación; con `@SessionScope`, cuando termina la sesión. |

Como `Invoice` es `@SessionScope` (ver la sección siguiente), al arrancar con
`./mvnw -DskipTests spring-boot:run` no aparece ningún log de `Invoice`. La
primera llamada a `GET /api/invoices` desde un navegador crea la factura de esa
sesión, y al detener la aplicación con Ctrl+C se destruye:

```
Constructor de Invoice: description = null
Constructor de Invoice: instancia = ...models.Invoice@2e8f41f2 | hashCode = 781140466
@PostConstruct: factura 'Compras de supermercado' | items: 10 | total: 37710
show(): instancia = ...models.Invoice@2e8f41f2 | hashCode = -534502517
@PreDestroy: el componente Invoice se va a destruir
```

### Alcance por sesión: `@SessionScope`

El alcance por defecto de un bean en Spring es **singleton**: una sola
instancia creada al arrancar (así es `Customer`). `Invoice`, en cambio, usa
`@SessionScope`: **una instancia por sesión HTTP**. Tomcat identifica la sesión
con la cookie `JSESSIONID`, así que todas las peticiones de un mismo navegador
comparten la misma factura, y otro navegador (o una ventana de incógnito) tiene
la suya.

```java
@Component
@SessionScope
@JsonIgnoreProperties({"targetSource", "advisors", "advisorCount", "exposeProxy", "frozen",
        "preFiltered", "proxiedInterfaces", "proxyTargetClass", "targetClass", "targetObject"})
public class Invoice {
```

`InvoiceServiceImpl` es singleton y recibe `Invoice` una sola vez al arrancar,
cuando todavía no hay ninguna sesión. Por eso Spring no inyecta el objeto real
sino un **proxy**: una subclase generada (`Invoice$$SpringCGLIB$$0`) que, en
cada llamada, busca la instancia de la sesión en curso y le delega el método.
Esto tiene tres consecuencias:

- **El ciclo de vida sigue a la sesión**: el constructor y `@PostConstruct` se
  ejecutan en la primera petición de cada sesión, y `@PreDestroy` cuando la
  sesión termina: expira (en este proyecto, 30 segundos sin actividad), se
  invalida o se detiene la aplicación; o cuando se cierra solo la factura con
  `POST /api/invoices/close`. Al arrancar no aparece ningún log de `Invoice`.
- **Jackson serializa el proxy**, que tiene getters internos de Spring
  (`getAdvisors()`, `getTargetSource()`, `isFrozen()`, `getTargetObject()`…).
  Algunos tienen referencias a sí mismos y la petición falla con
  `Direct self-reference leading to cycle`. `@JsonIgnoreProperties` le indica a
  Jackson que ignore esas propiedades, y el JSON queda solo con `customer`,
  `description`, `items` y `total`.
- **`toString()` y `hashCode()` no coinciden en `show()`**, como se ve abajo.

`show()` en `InvoiceController` registra la factura que devuelve:

```java
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
```

`toString()` se llama antes de `log.info` a propósito. En la primera petición
de cada sesión, esa llamada es la que crea el objeto real (constructor y
`@PostConstruct`). Si se pasara el proxy directo a `log.info`, Logback llamaría
a `toString()` mientras escribe el mensaje, y descarta los logs que se generan
en ese momento: los del constructor y de `@PostConstruct` no aparecerían.

Con dos peticiones desde un navegador (A), una desde otro (B) y luego Ctrl+C,
el log muestra algo así:

```
-- navegador A, petición 1 (crea la sesión y su factura) --
Constructor de Invoice: instancia = ...models.Invoice@2e8f41f2 | hashCode = 781140466
@PostConstruct: factura 'Compras de supermercado' | items: 10 | total: 37710
show(): instancia = ...models.Invoice@2e8f41f2 | hashCode = -534502517
-- navegador A, petición 2 (misma sesión, misma factura) --
show(): instancia = ...models.Invoice@2e8f41f2 | hashCode = -534502517
-- navegador B, petición 1 (otra sesión, otra factura) --
Constructor de Invoice: instancia = ...models.Invoice@238f2c25 | hashCode = 596585509
@PostConstruct: factura 'Compras de supermercado' | items: 10 | total: 37710
show(): instancia = ...models.Invoice@238f2c25 | hashCode = -534502517
-- Ctrl+C: terminan las dos sesiones --
@PreDestroy: el componente Invoice se va a destruir
@PreDestroy: el componente Invoice se va a destruir
```

- `Invoice@2e8f41f2` es el `toString()` por defecto de Java (clase y `hashCode`
  en hexadecimal). El proxy lo delega al objeto real, por eso coincide con el
  del constructor: **se repite dentro de la misma sesión y cambia entre
  sesiones**.
- `hashCode()` en cambio lo responde el proxy, por eso es otro número y es
  **igual en todas las peticiones**: el proxy es uno solo.
- Para probarlo con dos sesiones, abre `/api/invoices` en una ventana normal y
  en una de incógnito.
- Los valores exactos cambian en cada ejecución.

#### Cerrar solo la factura: `POST /api/invoices/close`

Para ver `@PreDestroy` sin detener el servidor, `InvoiceController` expone un
endpoint que cierra la factura de la sesión actual. **No invalida la sesión**,
porque en una aplicación real la sesión puede guardar otros datos (usuario,
carrito, preferencias): quita solo la factura.

```java
// InvoiceController
@PostMapping("/close")
public ResponseEntity<Void> close() {
    log.info("close(): cerrando la factura de la sesión");
    invoiceService.closeInvoice();
    return ResponseEntity.noContent().build();
}
```

```java
// InvoiceServiceImpl
@Override
public void closeInvoice() {
    RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
    attributes.removeAttribute(ScopedProxyUtils.getTargetBeanName("invoice"), RequestAttributes.SCOPE_SESSION);
}
```

- Un bean `@SessionScope` se guarda en la sesión como un atributo más, con el
  nombre `scopedTarget.invoice` (`ScopedProxyUtils.getTargetBeanName("invoice")`).
  `removeAttribute(..., SCOPE_SESSION)` quita solo ese atributo, y Spring
  ejecuta el `@PreDestroy` de la factura en el momento.
- La sesión sigue viva: la siguiente petición con la misma cookie usa la misma
  sesión y crea una factura nueva.
- Si no hay sesión (petición sin cookie), no hace nada y no crea una sesión.
- Es `POST` porque cambia el estado del servidor; responde `204 No Content`.
- Se descartó `ScopedObject.removeFromScope()` del proxy: ejecuta el
  `@PreDestroy` dos veces y, si no hay sesión, crea una solo para borrarla.

Para probarlo con `curl` (el archivo `cookies.txt` guarda la cookie de sesión,
como haría un navegador o Postman):

```bash
curl -b cookies.txt -c cookies.txt http://localhost:8080/api/invoices
```

```bash
curl -X POST -b cookies.txt -c cookies.txt http://localhost:8080/api/invoices/close
```

```bash
curl -b cookies.txt -c cookies.txt http://localhost:8080/api/invoices
```

El log muestra (la tercera petición usa la misma sesión, sin una cookie nueva):

```
Constructor de Invoice: instancia = ...models.Invoice@12967f14 | hashCode = 311852820
show(): instancia = ...models.Invoice@12967f14 | hashCode = -2009169786
close(): cerrando la factura de la sesión
@PreDestroy: el componente Invoice se va a destruir
Constructor de Invoice: instancia = ...models.Invoice@6324b5de | hashCode = 1663350238
show(): instancia = ...models.Invoice@6324b5de | hashCode = -2009169786
```

#### Configuración de sesiones para la demo

`application.properties` ajusta tres cosas de las sesiones:

```properties
server.servlet.session.persistent=false
app.session.timeout=30s
server.tomcat.background-processor-delay=5s
```

**`server.servlet.session.persistent=false`**: devtools activa por defecto la
persistencia de sesiones para no perderlas en cada recarga. Al detener la
aplicación, Tomcat guarda las sesiones en disco en lugar de cerrarlas, y así el
`@PreDestroy` no aparece con Ctrl+C. Desactivada, al detener se cierran todas
las sesiones y se ejecuta el `@PreDestroy` de cada factura. A cambio, con
devtools cada recarga empieza con sesiones nuevas.

**`app.session.timeout=30s`**: la sesión expira tras 30 segundos sin
peticiones, en vez de los 30 minutos por defecto. No se usa
`server.servlet.session.timeout` porque Spring Boot lo redondea a minutos en
Tomcat (mínimo 1 minuto). En su lugar, `AppConfig` registra un
`HttpSessionListener` que aplica el valor en segundos a cada sesión nueva:

```java
@Bean
public HttpSessionListener sessionTimeoutListener(@Value("${app.session.timeout}") Duration timeout) {
    return new HttpSessionListener() {
        @Override
        public void sessionCreated(HttpSessionEvent event) {
            event.getSession().setMaxInactiveInterval((int) timeout.toSeconds());
        }
    };
}
```

**`server.tomcat.background-processor-delay=5s`**: Tomcat no detecta una
sesión vencida al instante, sino en una tarea en segundo plano que revisa las
sesiones cada 6 ejecuciones de su proceso de fondo. Con 5 s, revisa cada 30 s
(por defecto, cada 60 s). Así, el `@PreDestroy` aparece **entre 30 y 60
segundos** después de la última petición.

> Borrar la cookie `JSESSIONID` en Postman o en el navegador **no** cierra la
> sesión: la cookie es solo el identificador que guarda el cliente. La sesión
> sigue viva en el servidor hasta que expira. Con esta configuración, después
> de borrar la cookie el `@PreDestroy` de esa factura aparece entre 30 y 60
> segundos más tarde. Para cerrarla en el momento, usa `POST /api/invoices/close`.

#### `ContextCleanupListener` (solo para `@ApplicationScope`)

`AppConfig` declara un bean `ContextCleanupListener`:

```java
@Bean
public ContextCleanupListener contextCleanupListener() {
    return new ContextCleanupListener();
}
```

Con `@SessionScope` (o `@RequestScope`) no se usa: Spring ejecuta el
`@PreDestroy` al terminar la sesión (o la petición). Hace falta con `@ApplicationScope`, donde el bean vive en el
`ServletContext` y su `@PreDestroy` se ejecuta al cerrarlo; Spring Boot con
Tomcat embebido no registra por defecto el listener que dispara ese cierre. Se
mantiene para poder volver a ese scope sin perder el `@PreDestroy`.

## Interceptor: tiempo de cada request

Un **interceptor** de Spring MVC (`HandlerInterceptor`) se ejecuta antes y
después de cada controller, sin tocar el código de los controllers. El
proyecto usa uno para medir cuánto tarda cada request.

### `LoadingTimeInterceptor`

```java
@Component
public class LoadingTimeInterceptor implements HandlerInterceptor {

    private static final String START_TIME = LoadingTimeInterceptor.class.getName() + ".startTime";
    private static final String ELAPSED_MS = LoadingTimeInterceptor.class.getName() + ".elapsedMs";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.nanoTime());
        log.info("preHandle(): entrando a {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long elapsedMs = elapsedMillis(request);
        if (elapsedMs != null) {
            log.info("afterCompletion(): {} {} -> status {} en {} ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
        }
    }

    // Se calcula una sola vez por request: la primera llamada fija el valor
    public static Long elapsedMillis(HttpServletRequest request) {
        if (request.getAttribute(ELAPSED_MS) instanceof Long elapsed) {
            return elapsed;
        }
        if (request.getAttribute(START_TIME) instanceof Long start) {
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            request.setAttribute(ELAPSED_MS, elapsed);
            return elapsed;
        }
        return null;
    }
}
```

| Método | Cuándo se ejecuta | Qué hace |
|---|---|---|
| `preHandle()` | Antes del controller | Guarda el instante de inicio. Devuelve `true` para que la petición siga; con `false` la cortaría. |
| `afterCompletion()` | Al terminar la petición, incluso si hubo una excepción | Registra el tiempo en el log. |

El tiempo de inicio se guarda como **atributo del request** y no en un campo de
la clase: el interceptor es un singleton compartido por todas las peticiones, y
con un campo dos peticiones simultáneas se pisarían el valor.

Se registra para todos los endpoints en `WebMvcConfig`:

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoadingTimeInterceptor loadingTimeInterceptor;

    public WebMvcConfig(LoadingTimeInterceptor loadingTimeInterceptor) {
        this.loadingTimeInterceptor = loadingTimeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loadingTimeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/app/date", "/api/app/languages");
    }
}
```

`addPathPatterns("/**")` aplica el interceptor a todos los endpoints, y
`excludePathPatterns(...)` deja fuera `/api/app/date` y `/api/app/languages`:
esas rutas no se miden, no aparecen en el log y su JSON no lleva `elapsedMs`.

El log muestra, por cada petición:

```
preHandle(): entrando a GET /api/invoices
afterCompletion(): GET /api/invoices -> status 200 en 35 ms
preHandle(): entrando a POST /api/invoices/close
afterCompletion(): POST /api/invoices/close -> status 204 en 1 ms
```

Los endpoints de Actuator (`/actuator/health`) no pasan por este interceptor,
porque Spring Boot los atiende por un mecanismo separado.

### El tiempo en el JSON: `ElapsedTimeResponseAdvice`

Un interceptor no puede agregar datos al JSON: cuando se ejecuta lo que viene
después del controller, el body de un `@RestController` ya se escribió en la
respuesta. Para eso se usa un **`ResponseBodyAdvice`**, que Spring ejecuta
después del controller y justo antes de escribir el body:

```java
@RestControllerAdvice(basePackages = "com.andres.course.claude.springboot.springdi.app.controllers")
public class ElapsedTimeResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // ... calcula elapsedMs con LoadingTimeInterceptor.elapsedMillis(...)
        JsonNode json = jsonMapper.valueToTree(body);
        if (json instanceof ObjectNode object) {
            object.put("elapsedMs", elapsedMs);
            return object;
        }
        // ...
    }
}
```

| Tipo de respuesta | Qué hace |
|---|---|
| Objeto JSON (como la factura) | Agrega `elapsedMs` al final, sin cambiar los demás campos. |
| Lista | La envuelve: `{ "data": [...], "elapsedMs": 12 }`. |
| Error `ProblemDetail` | Agrega `elapsedMs` como una propiedad más. |
| Sin body (`204 No Content`) o no JSON | No la toca. |

- Se limita con `basePackages` a los controllers del proyecto: así no altera el
  JSON de Swagger (`/v3/api-docs`) ni los errores que Spring Boot genera fuera
  de los controllers (por ejemplo, el 404 de una ruta que no existe).
- `elapsedMs` y el log muestran **exactamente el mismo número**.
  `elapsedMillis(request)` calcula el tiempo una sola vez por request y lo
  guarda: el advice lo pide primero, al armar el JSON, y `afterCompletion()`
  reutiliza ese valor. Por eso el log mide hasta que se arma el JSON, sin el
  tiempo de escribir la respuesta (casi siempre menos de 1 ms). En respuestas
  sin body, como el 204 de `/close`, se calcula al terminar la petición.

| Petición | `elapsedMs` en el JSON | Log |
|---|---|---|
| `GET /api/invoices` | 28 | `status 200 en 28 ms` |
| `GET /api/app/greeting` | 2 | `status 200 en 2 ms` |
| `POST /api/invoices/close` | sin body | `status 204 en 1 ms` |

## Controller de ejemplo: `AppController`

Controller simple bajo `/api/app` con tres endpoints, para ver el interceptor
en acción. No tiene service porque no hay lógica de negocio.

```java
@RestController
@RequestMapping("/api/app")
public class AppController {

    @GetMapping("/greeting")
    public Map<String, String> greeting(@RequestParam(defaultValue = "mundo") String name) {
        return Map.of("message", "Hola, " + name + "!");
    }

    @GetMapping("/date")
    public Map<String, Object> date() {
        return Map.of("date", LocalDate.now(), "dateTime", LocalDateTime.now());
    }

    @GetMapping("/languages")
    public List<String> languages() {
        return List.of("Java", "Kotlin", "JavaScript", "TypeScript", "Python", "Go");
    }
}
```

| Endpoint | Interceptor | Respuesta |
|---|---|---|
| `GET /api/app/greeting` | Sí | `{"message":"Hola, mundo!","elapsedMs":13}` |
| `GET /api/app/greeting?name=Ana` | Sí | `{"message":"Hola, Ana!","elapsedMs":1}` |
| `GET /api/app/date` | Excluido | `{"date":"2026-11-13","dateTime":"2026-11-13T19:19:23.675031"}` |
| `GET /api/app/languages` | Excluido | `["Java","Kotlin","JavaScript","TypeScript","Python","Go"]` |

- `greeting` y `date` devuelven un `Map` para que la respuesta sea un objeto
  JSON; con un `String` sería texto plano y el advice no le agregaría `elapsedMs`.
- `languages` devuelve la lista tal cual: como está excluida del interceptor,
  no se envuelve en `{ "data": [...] }`.
- `Map.of` no garantiza el orden de los campos, así que en `date` pueden salir
  en cualquier orden.

## Uso

Abre Claude Code en este directorio y pídele lo que necesites en lenguaje
natural. La skill se activa sola cuando la petición encaja con sus reglas
(definidas en [CLAUDE.md](CLAUDE.md)).

**Crear el proyecto** (ya hecho en este repositorio; sirve como referencia para
otros proyectos del curso):

```text
Crea una estructura de Spring Boot para esta carpeta.
```

```text
Crea una API REST básica con Spring Boot para gestionar productos con nombre, descripción y precio.
```

**Agregar o modificar capas:**

```text
Agrega un entity Category con su repository, service y controller.
```

```text
Agrega el campo stock al producto.
```

**Agregar persistencia:**

```text
Agrega Spring Data JPA con PostgreSQL y convierte Product en una entidad JPA.
```

También puedes generar solo el esqueleto ejecutando el script directamente:

```bash
bash .claude/skills/spring-boot-best-practices/scripts/create_project.sh .
```

Opciones del script (se pueden combinar):

| Flag | Agrega |
|---|---|
| *(ninguno)* | API REST sin persistencia |
| `--with-jpa` | Spring Data JPA + Hibernate + PostgreSQL |
| `--with-thymeleaf` | Thymeleaf para vistas web |

El script nunca sobrescribe archivos existentes y se detiene si ya hay un `pom.xml`.

## Arquitectura de capas

Estructura completa que sigue la skill a medida que se agregan recursos
(`config/` y `controllers/web/` aparecen solo cuando hacen falta):

```
com.andres.course.claude.springboot.{artifactId}.app/
├── {ArtifactId}Application.java
├── config/          # beans de configuración (p. ej. PasswordEncoder)
├── models/          # entidades
├── dto/             # records: un solo DTO para request y response
├── mappers/         # conversión explícita entity <-> dto
├── repositories/    # acceso a datos
├── services/        # interface + implementación (*ServiceImpl)
├── controllers/     # @RestController
│   └── web/         # @Controller de vistas (monolito)
└── exceptions/      # excepciones de dominio + @RestControllerAdvice
```

Responsabilidades de cada capa:

- **Controller:** solo expone endpoints REST, valida la entrada con `@Valid`,
  delega en el service y devuelve DTOs. No contiene lógica de negocio ni accede
  a repositorios.
- **Service:** contrato (interface) + implementación. Contiene la lógica de
  negocio, orquesta los repositorios y convierte entre entidades y DTOs usando
  el mapper.
- **Repository:** interfaz que extiende `JpaRepository` cuando hay persistencia,
  o una interfaz con implementación en memoria cuando no la hay.
- **DTO:** `record`, el mismo para request y response. No incluye los campos de
  auditoría (`created_at`, `updated_at`). Un `password` se recibe como campo
  solo de escritura (`@JsonProperty(access = WRITE_ONLY)`) y nunca se devuelve.
- **Mapper:** clase `@Component` con `toDto(entity)`, `toEntity(dto)` y
  `updateEntity(dto, entity)`.
- **Excepciones:** `@RestControllerAdvice` que responde con `ProblemDetail`
  (404, 400 de validación, 409 para duplicados).

Todas las dependencias se inyectan por constructor con campos `final`.

## Persistencia bajo demanda

Por defecto el proyecto **no** incluye Spring Data JPA ni PostgreSQL, y los
repositorios guardan los datos en memoria (se pierden al reiniciar). Los
métodos del repositorio en memoria se llaman igual que los de `JpaRepository`,
así que al migrar solo cambia esa capa.

La persistencia se agrega cuando pides:

- Spring Data JPA, Hibernate, JPA o persistencia.
- Una base de datos o PostgreSQL.
- Un repositorio con `CrudRepository` o `JpaRepository`.
- Una clase entity mapeada con JPA/Hibernate (`@Entity`).

La conexión se configura con variables de entorno:

| Variable | Valor por defecto |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/{db_name}` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |

Los valores por defecto son solo para desarrollo local. Para levantar
PostgreSQL con Docker:

```bash
docker run --name spring-di-db -e POSTGRES_DB=spring_di -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:18
```

## Vistas web (monolito)

En un monolito con Spring Web se usan plantillas Thymeleaf con Tailwind CSS y
un layout compartido con HTML semántico (`header`, `nav`, `main`, `footer`):

```
src/main/resources/templates/
├── layouts/base.html
├── fragments/header.html
├── fragments/footer.html
├── index.html
└── {recurso}/list.html, form.html
```

## Ejecutar la aplicación

```bash
./mvnw -DskipTests spring-boot:run
```

Solo compilar:

```bash
./mvnw -q -DskipTests compile
```

Ejecutar los tests:

```bash
./mvnw test
```

Si el proyecto usa JPA, PostgreSQL debe estar disponible antes de arrancar la
aplicación o de ejecutar los tests.

## Endpoints útiles

| URL | Descripción |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | Especificación OpenAPI |
| http://localhost:8080/actuator/health | Estado de la aplicación |
| http://localhost:8080/actuator/info | Información de la aplicación |
| http://localhost:8080/api/invoices | Factura de ejemplo con su cliente e items |
| `POST` http://localhost:8080/api/invoices/close | Cierra solo la factura de la sesión actual (ejecuta su `@PreDestroy`) |
| http://localhost:8080/api/app/greeting | Saludo (`?name=` opcional), con `elapsedMs` |
| http://localhost:8080/api/app/date | Fecha y fecha-hora del servidor (excluido del interceptor) |
| http://localhost:8080/api/app/languages | Lista de lenguajes (excluido del interceptor) |

## Pruebas de la skill

La skill se probó con tres casos (API sin base de datos, API con JPA y
PostgreSQL, y monolito con Thymeleaf), comparando los resultados con y sin la
skill:

| Configuración | Verificaciones cumplidas |
|---|---|
| Con la skill (iteración 2) | 56/56 (100 %) |
| Sin la skill | 37/56 (66 %) |

Los casos están en `.claude/skills/spring-boot-best-practices/evals/evals.json`,
y los resultados y el calificador en `.claude/skills/spring-boot-best-practices-workspace/`.
Ninguno de los dos es necesario para usar la skill, y ambos están excluidos del
repositorio en `.gitignore`.
