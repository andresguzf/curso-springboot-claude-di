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
│   │   │   ├── AppConfig.java                # @Bean con las listas de items (PC, supermercado, JSON y CSV)
│   │   │   └── InvoiceItemsSource.java       # calificador personalizado para elegir la lista
│   │   ├── models/
│   │   │   ├── Customer.java                 # componente: nombre y apellido del cliente
│   │   │   ├── Invoice.java                  # componente: descripción + Customer e items inyectados + total + ciclo de vida
│   │   │   ├── InvoiceItem.java              # item: producto, cantidad y total
│   │   │   └── Product.java                  # producto: nombre, precio y fechas
│   │   ├── dto/package-info.java             # DTOs como record (vacío por ahora)
│   │   ├── mappers/package-info.java         # conversión entity <-> dto (vacío por ahora)
│   │   ├── repositories/package-info.java    # acceso a datos (vacío por ahora)
│   │   ├── services/
│   │   │   ├── InvoiceService.java           # contrato del service
│   │   │   └── InvoiceServiceImpl.java       # implementación (@Service)
│   │   ├── controllers/
│   │   │   └── InvoiceController.java        # GET /api/invoices
│   │   └── exceptions/
│   │       ├── ResourceNotFoundException.java
│   │       └── GlobalExceptionHandler.java   # ProblemDetail: 404 y 400 de validación
│   └── resources/
│       ├── application.properties            # Swagger UI, Actuator, ProblemDetail y datos de ejemplo
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
  "total": 37710
}
```

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
| `@PostConstruct` | Se ejecuta una vez, cuando ya están inyectadas todas las dependencias y los `@Value`. Es el lugar para inicializar usando esos valores. |
| `@PreDestroy` | Se ejecuta al cerrar el contexto (al detener la aplicación), antes de destruir el bean. Sirve para liberar recursos. |

Al arrancar con `./mvnw -DskipTests spring-boot:run` y detener con Ctrl+C, el
log muestra:

```
Constructor de Invoice: description = null
@PostConstruct: factura 'Compras de supermercado' | items: 10 | total: 37710
@PreDestroy: el componente Invoice se va a destruir
```

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
