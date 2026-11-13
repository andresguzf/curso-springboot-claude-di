# CLAUDE.md

## Proyecto

API REST con Spring Boot del curso (proyecto `2-spring-di`), generada con Spring
Initializr mediante la skill `spring-boot-best-practices`.

| | |
|---|---|
| Spring Boot | 4.1.2 |
| Java | 25 |
| Build | Maven con Maven Wrapper (`./mvnw`), packaging jar |
| Configuración | `src/main/resources/application.properties` (no YAML) |
| groupId / artifactId | `com.andres.course.claude.springboot` / `2-spring-di` |
| Package base | `com.andres.course.claude.springboot.springdi.app` |
| Dependencias | webmvc, validation, springdoc-openapi (Swagger UI), devtools, actuator |
| Persistencia | ninguna todavía: sin JPA ni PostgreSQL (repositorios en memoria) |

## Estructura actual

```
2-spring-di/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/wrapper/       # Maven Wrapper
├── src/main/java/com/andres/course/claude/springboot/springdi/app/
│   ├── Application.java                # @SpringBootApplication
│   ├── config/
│   │   ├── AppConfig.java              # @Configuration: @Bean itemsOffice (3 PC), itemsSupermarket (7), itemsJson (8, JSON, @Primary) e itemsCsv (10, CSV)
│   │   └── InvoiceItemsSource.java     # calificador personalizado: @InvoiceItemsSource(Source.OFFICE | SUPERMARKET | JSON | CSV)
│   ├── models/
│   │   ├── Customer.java               # @Component: name, lastname (@Value)
│   │   ├── Invoice.java                # @Component: description (@Value) + Customer e items (@InvoiceItemsSource) por constructor; getTotal(); @PostConstruct / @PreDestroy
│   │   ├── InvoiceItem.java            # POJO: product, quantity; getTotal() = precio × cantidad
│   │   └── Product.java                # POJO: name, price (BigDecimal), createdAt, updatedAt
│   ├── dto/                            # vacío (package-info.java)
│   ├── mappers/                        # vacío (package-info.java)
│   ├── repositories/                   # vacío (package-info.java)
│   ├── services/
│   │   ├── InvoiceService.java         # contrato: Invoice getInvoice()
│   │   └── InvoiceServiceImpl.java     # @Service, recibe Invoice por constructor
│   ├── controllers/
│   │   └── InvoiceController.java      # GET /api/invoices, recibe InvoiceService por constructor; registra instancia y hashCode
│   └── exceptions/
│       ├── ResourceNotFoundException.java
│       └── GlobalExceptionHandler.java # @RestControllerAdvice -> ProblemDetail (404, 400)
├── src/main/resources/application.properties   # springdoc, actuator, problemdetails, datos de ejemplo
├── src/main/resources/data/invoice-items.json  # 8 items de supermercado que lee el bean itemsJson
├── src/main/resources/data/invoice-items.csv   # 10 items de supermercado que lee el bean itemsCsv
├── src/test/java/.../springdi/app/ApplicationTests.java
└── .claude/skills/spring-boot-best-practices/  # skill del proyecto
```

La clase principal se llama `Application.java` (y no `SpringDiApplication`)
porque Initializr no genera un nombre de clase a partir de un artifactId que
empieza con número. No la renombres.

### Ejemplo de inyección de dependencias (Invoice / Customer)

El proyecto es un ejemplo didáctico de DI, construido paso a paso:

- `Customer` e `Invoice` son **beans `@Component`** (singleton), no entidades de
  datos. Sus valores vienen de `application.properties` (`customer.name`,
  `customer.lastname`, `invoice.description`) mediante `@Value`.
- `Product` e `InvoiceItem` son **POJOs** (no beans): hay varias instancias por
  clase. Las listas de items se crean en `AppConfig` con métodos `@Bean`:
  `itemsOffice` (3 componentes de PC), `itemsSupermarket` (7 productos de
  supermercado), `itemsJson` (8 productos leídos de
  `classpath:data/invoice-items.json`) e `itemsCsv` (10 productos leídos de
  `classpath:data/invoice-items.csv`).
- `itemsJson` recibe por parámetro el `JsonMapper` de Jackson 3 que configura
  Spring Boot (`tools.jackson.databind.json.JsonMapper`) y el archivo como
  `Resource` con `@Value("classpath:...")`; lo deserializa con `TypeReference`.
  Si el archivo no se puede leer, lanza `UncheckedIOException` y la app no arranca.
- `itemsCsv` lee el CSV en UTF-8 con `BufferedReader`, salta el encabezado
  (`name,price,quantity,createdAt,updatedAt`), ignora líneas vacías y convierte
  cada línea con el método privado `toInvoiceItem` (`split(",")`, sin librerías).
  Los nombres de producto no deben contener comas.
- Como hay cuatro beans `List<InvoiceItem>`, se distinguen con el calificador
  personalizado `@InvoiceItemsSource` (meta-anotado con `@Qualifier`), cuyo
  valor es el enum `InvoiceItemsSource.Source` (`OFFICE`, `SUPERMARKET`,
  `JSON`, `CSV`). Cada `@Bean` de `AppConfig` lleva su origen, e `Invoice` pide
  el suyo en el parámetro del constructor (actualmente `Source.CSV`). Para usar
  otra lista basta cambiar el valor del enum. Usa este calificador en vez de
  `@Qualifier("nombre")` para las listas de items.
- `itemsJson` es `@Primary`: es la lista que se inyecta cuando se pide
  `List<InvoiceItem>` sin calificador. Un calificador explícito
  (`@InvoiceItemsSource`) siempre tiene prioridad sobre `@Primary`. No quites
  `@Primary` de `itemsJson`.
- Totales calculados (no se guardan): `InvoiceItem.getTotal()` = precio del
  producto × cantidad; `Invoice.getTotal()` = suma de los totales de los items
  con `reduce(BigDecimal.ZERO, BigDecimal::add)` (la alternativa con
  `mapToDouble().sum()` queda comentada en el código).
  Ambos son `BigDecimal` y Jackson los incluye en el JSON como `total`.
- Ciclo de vida de `Invoice` (`jakarta.annotation`, ya incluida por los
  starters): el constructor registra que `description` aún es `null` (los
  `@Value` se inyectan después); `init()` con `@PostConstruct` registra un
  resumen corto (descripción, cantidad de items y total); `destroy()` con
  `@PreDestroy` solo avisa que el componente se va a destruir. Los logs usan
  SLF4J. Mantén esos mensajes cortos.
- Demostración de singleton: el constructor de `Invoice` y `show()` de
  `InvoiceController` registran la instancia (`toString()` por defecto,
  `Clase@hashCode` en hexadecimal) y su `hashCode()`. Siempre es la misma
  instancia. No sobrescribas `toString()` en `Invoice`: ocultaría esa información.
- Cadena de inyección: `Customer` + `List<InvoiceItem>` → `Invoice` →
  `InvoiceServiceImpl` → `InvoiceController`. El controller depende de la
  interfaz `InvoiceService`.
- `GET /api/invoices` devuelve el bean `Invoice` serializado a JSON, con
  cliente, descripción, items con su total y el total de la factura (incluye
  `createdAt`/`updatedAt` del producto porque aún no hay DTOs).
- Por decisión del curso, en esta etapa **no** hay DTOs, mapper ni repository:
  el service devuelve `Invoice` directamente. No los agregues salvo que el
  usuario lo pida.

### Convenciones del proyecto

- Inyección **por constructor** con campos `final` y sin `@Autowired` (un solo
  constructor). No uses inyección por atributo ni por setter.
- Los packages `dto`, `mappers` y `repositories` siguen vacíos (solo
  `package-info.java`); se llenan cuando el curso llegue a esas capas.

## Comandos

- Ejecutar: `./mvnw -DskipTests spring-boot:run`
- Compilar: `./mvnw -q -DskipTests compile`
- Tests: `./mvnw test`
- Swagger UI: http://localhost:8080/swagger-ui.html · Health: http://localhost:8080/actuator/health
- Endpoint de ejemplo: http://localhost:8080/api/invoices

## Available skills

| Skill | Ubicación | Propósito |
|---|---|---|
| `spring-boot-best-practices` | `.claude/skills/spring-boot-best-practices/` | Crea proyectos Spring Boot con Spring Initializr (Java 25, Spring Boot 4.1.2+, Maven, jar, `application.properties`) y mantiene la arquitectura controllers / services (interface + impl) / repositories / models, con DTOs `record`, Mapper explícito, JPA + PostgreSQL bajo demanda y vistas Thymeleaf + Tailwind. |

## Skill trigger rules

Usa `spring-boot-best-practices` cuando el usuario:

- Pida crear una API REST básica con Spring Boot.
- Pida crear un monolito con Spring Web / Spring MVC (con vistas Thymeleaf).
- Pida crear, agregar o modificar un entity, model, DTO, mapper, repository, service o controller de Spring.
- Pida agregar persistencia a un proyecto Spring Boot: Spring Data JPA, Hibernate, PostgreSQL o base de datos, un repositorio con `CrudRepository` / `JpaRepository`, o una clase entity mapeada con JPA/Hibernate (`@Entity`).

Reglas clave de la skill:

- Spring Data JPA y PostgreSQL **no** se incluyen por defecto; se agregan solo cuando se pide persistencia (JPA, Hibernate, base de datos, `CrudRepository` / `JpaRepository` o una entity `@Entity`).
- Para ejecutar la aplicación localmente usa siempre: `./mvnw -DskipTests spring-boot:run`.
