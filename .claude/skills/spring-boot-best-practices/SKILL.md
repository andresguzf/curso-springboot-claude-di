---
name: spring-boot-best-practices
description: Genera y mantiene proyectos Spring Boot (Java 25, Spring Boot 4.1.2+, Maven, jar, application.properties) creados con Spring Initializr, con arquitectura en capas controllers / services (interface + impl) / repositories / models, DTOs como record y Mapper explícito; agrega Spring Data JPA + Hibernate + PostgreSQL solo bajo demanda (cuando se pide JPA, Hibernate, base de datos, un CrudRepository/JpaRepository o una clase @Entity), y vistas Thymeleaf + Tailwind para monolitos. Úsala siempre que el usuario pida crear una API REST básica con Spring Boot, un monolito Spring Web/MVC, o crear, agregar o modificar un entity, DTO, mapper, repository, service o controller de Spring, aunque no mencione esta skill ni diga "buenas prácticas".
---

# Spring Boot best practices

Esta skill define cómo crear un proyecto Spring Boot desde cero y cómo agregar o
modificar sus capas para que todos los proyectos del curso queden con la misma
estructura, las mismas convenciones y versiones actuales. Se usa en dos
situaciones:

1. **Proyecto nuevo**: el workspace no tiene `pom.xml` → genera el proyecto (paso 1) y luego las capas.
2. **Proyecto existente**: ya hay `pom.xml` → salta al paso 3 y respeta el package base que ya existe.

## 1. Generar el proyecto con Spring Initializr

Usa siempre la herramienta oficial https://start.spring.io (no escribas el
`pom.xml` a mano): así el wrapper, el parent y las versiones de los starters
quedan coherentes con la versión de Boot. El script incluido lo hace en el
directorio del workspace sin sobrescribir archivos existentes. Las rutas de
abajo son relativas a la raíz del proyecto; si trabajas desde otro directorio,
usa la ruta absoluta del directorio de esta skill:

```bash
bash .claude/skills/spring-boot-best-practices/scripts/create_project.sh .                    # API REST sin persistencia
bash .claude/skills/spring-boot-best-practices/scripts/create_project.sh . --with-jpa         # + JPA/Hibernate + PostgreSQL
bash .claude/skills/spring-boot-best-practices/scripts/create_project.sh . --with-thymeleaf   # + vistas (monolito web)
```

Los flags se combinan. Usa `--with-jpa` **solo** si el usuario pide
persistencia (ver "Cuándo agregar persistencia" abajo). Sin esa petición el proyecto se crea sin `data-jpa` ni
`postgresql`, porque un starter JPA sin base de datos configurada impide que la
aplicación arranque y agrega complejidad que la clase aún no necesita.

Qué configura (y por qué):

| Opción | Valor |
|---|---|
| Build | Maven (`type=maven-project`) con Maven Wrapper: `.mvn/`, `./mvnw`, `./mvnw.cmd` |
| Java | 25 |
| Spring Boot | última GA publicada en start.spring.io, mínimo 4.1.2 (el script la consulta en los metadatos; nunca SNAPSHOT/RC/M) |
| Packaging | jar |
| Config | `application.properties` (no YAML) |
| groupId | `com.andres.course.claude.springboot` |
| artifactId / name | nombre del directorio del workspace |
| package base | `com.andres.course.claude.springboot.{artifactId}.app` |
| Dependencias base | `web`, `validation`, `springdoc-openapi` (OpenAPI UI), `devtools`, `actuator` |
| Bajo demanda | `data-jpa` + `postgresql` (`--with-jpa`), `thymeleaf` (`--with-thymeleaf`) |

### Cuándo agregar persistencia

Agrega `data-jpa` + `postgresql` (al crear con `--with-jpa`, o después en el
`pom.xml` de un proyecto existente) cuando el usuario pida algo como:

- Agregar Spring Data JPA, Hibernate, JPA o persistencia.
- Usar una base de datos / PostgreSQL / guardar datos en BD.
- Crear o agregar un repositorio con `CrudRepository`, `ListCrudRepository` o `JpaRepository`.
- Crear una clase entity mapeada con JPA/Hibernate (`@Entity`, `@Table`, `@Id`...).

Si solo pide "un repository" o "un model" sin mencionar nada de lo anterior, el
proyecto sigue sin persistencia y se usa el repositorio en memoria.

El `{artifactId}` del package se sanea porque un package Java no admite guiones
ni empezar con dígito: minúsculas, sin prefijo numérico y solo alfanuméricos
(`2-spring-di` → `com.andres.course.claude.springboot.springdi.app`).

Si el script no puede usarse (sin red, sin `python3`), arma la misma petición a
`https://start.spring.io/starter.zip` con esos parámetros. Después verifica que
existan `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties`.

Notas de Spring Boot 4 que conviene tener presentes: el starter web se llama
`spring-boot-starter-webmvc`, cada starter trae su propio `-test`, se usa
Jakarta EE 11 (`jakarta.*`), Hibernate 7 y Jackson 3 (`tools.jackson.*`).

## 2. Configurar `application.properties`

Agrega springdoc y actuator siempre; datasource PostgreSQL y JPA solo si el
proyecto tiene persistencia. Nunca escribas
contraseñas reales: usa variables de entorno con valores por defecto genéricos.
Plantilla completa en `references/configuration.md`.

## 3. Estructura de packages

```
{package-base}/
├── {ArtifactId}Application.java   # nombre que genera Initializr, no lo renombres
├── config/          # beans de configuración (p. ej. PasswordEncoder), si hacen falta
├── models/          # entidades (POJO sin JPA, @Entity con JPA)
├── dto/             # records: mismo DTO para request y response
├── mappers/         # conversión explícita entity <-> dto (@Component)
├── repositories/    # interfaces (JpaRepository con JPA; impl en memoria sin JPA)
├── services/        # contrato (interface) + implementación (*ServiceImpl)
├── controllers/     # @RestController: solo endpoints REST
│   └── web/         # @Controller de vistas Thymeleaf (solo monolito)
└── exceptions/      # excepciones de dominio + @RestControllerAdvice
```

Crea `models`, `controllers`, `repositories` y `services` siempre, aunque al
principio no haya entidades (un `package-info.java` basta para que existan).

## 4. Responsabilidades por capa

La idea central es que cada capa tenga una sola razón para cambiar y que las
entidades JPA nunca salgan del service:

La persistencia cambia cómo se implementan `models` y `repositories`; el resto
de capas es igual en ambos casos (por eso el service depende de la interfaz del
repositorio y no de su implementación):

| | Sin persistencia (por defecto) | Con JPA (`--with-jpa`) |
|---|---|---|
| models | POJO simple, sin anotaciones JPA | `@Entity` |
| repositories | interfaz propia + `*RepositoryImpl` en memoria (`@Repository`, `ConcurrentHashMap` + `AtomicLong`) | interfaz que extiende `JpaRepository<X, Long>` |
| transacciones | sin `@Transactional` (no hay `spring-tx`) | `@Transactional` en el service |

- **models** — con JPA: entidades `jakarta.persistence`. Id `Long` con
  `GenerationType.IDENTITY`; auditoría con `@CreationTimestamp` /
  `@UpdateTimestamp` en `created_at` / `updated_at`. Constructor vacío
  requerido por JPA, getters/setters.
- **dto** — `record`, **uno solo por recurso**, usado tanto en request como en
  response (no crees `XCreateRequest`, `XResponse`, `PasswordDto`, etc.). Lleva
  las anotaciones de Bean Validation. **Excluye** los campos de auditoría
  `created_at`, `updated_at`, `create_at`, `update_at` (y sus variantes
  camelCase). El `id` va en el record pero se ignora al crear. Los campos
  sensibles que el cliente debe enviar, como `password`, van en el mismo record
  como **campo solo de escritura** (`@JsonProperty(access = WRITE_ONLY)`): se
  aceptan en el request pero nunca salen en la respuesta. Ver sección 8 de
  `references/layers.md`.
- **mappers** — clase `@Component` (p. ej. `ProductMapper`) con `toDto(entity)`
  y `toEntity(dto)`, más `updateEntity(dto, entity)` para actualizar sin perder
  campos que el DTO no trae (timestamps). El mapper nunca copia el password en
  ninguna dirección: lo hashea y asigna el service. Mapeo explícito, a mano, sin
  MapStruct ni reflexión: se lee y depura fácil.
- **repositories** — con JPA: `interface XRepository extends JpaRepository<X, Long>`
  (o `CrudRepository<X, Long>` si el usuario lo pide explícitamente),
  consultas derivadas por nombre (`findByEmail`, `existsBySku`) antes que
  `@Query`. Sin JPA: interfaz con los mismos nombres de método (`findAll`,
  `findById` → `Optional`, `save`, `delete`) e implementación en memoria,
  así migrar a JPA después solo cambia esta capa.
- **services** — interfaz `XService` (el contrato) + `XServiceImpl` con
  `@Service`. Contiene la lógica de negocio, orquesta repositorios, usa el
  mapper y **solo recibe y devuelve DTOs**. Con JPA, `@Transactional(readOnly = true)`
  en lecturas y `@Transactional` en escrituras. Lanza excepciones de dominio
  (`ResourceNotFoundException`) en vez de devolver `null`.
- **controllers** — `@RestController` bajo `/api/{recurso}`. Solo expone
  endpoints, valida con `@Valid`, delega al service (inyecta la interfaz, no la
  impl) y devuelve DTOs con `ResponseEntity` y el status correcto (201 +
  `Location` al crear, 204 al borrar). Sin lógica de negocio ni acceso a
  repositorios.
- **exceptions** — `@RestControllerAdvice` que traduce excepciones a
  `ProblemDetail` (404 not found, 400 validación).

Usa siempre inyección por constructor con campos `final` (sin `@Autowired` en
campos): hace explícitas las dependencias y facilita los tests.

Plantillas de código para cada capa (entity, record, mapper, repository,
service + impl, controller, advice) en `references/layers.md`. Léelo antes de
escribir o modificar cualquiera de estas clases.

## 5. Vistas web (monolito Spring Web)

Si el usuario pide una interfaz web o un monolito, usa Thymeleaf + Tailwind CSS
con un layout compartido y HTML semántico (`header`, `nav`, `main`, `footer`):

```
src/main/resources/templates/
├── layouts/base.html
├── fragments/header.html   # header + navbar
├── fragments/footer.html
└── {recurso}/list.html, form.html ...
```

Los `@Controller` de vistas viven en `controllers/web` y usan el mismo service y
DTO. En un monolito crea solo las páginas pedidas y un `HomeController` para `/`;
agrega además un `@RestController` solo si el usuario pide una API. Plantillas en
`references/thymeleaf.md`.

## 6. Modificar o agregar a un proyecto existente

- Detecta el package base desde la clase `@SpringBootApplication`; no lo cambies.
- Revisa el `pom.xml` para saber si hay JPA. Si el usuario pide persistencia y
  no está, agrégala (ver "Agregar JPA + PostgreSQL a un proyecto existente" en
  `references/configuration.md`) y migra los repositorios en memoria a
  `JpaRepository`. Si no la pide, no la agregues.
- Al agregar un campo a una entidad, actualiza en el mismo cambio: DTO (si no es
  sensible), mapper (`toDto`, `toEntity`, `updateEntity`) y vistas si las hay.
- Al agregar un recurso nuevo, crea el conjunto completo: entity, record, mapper,
  repository, service + impl y controller (REST para una API, de vistas para un monolito).
- Si un recurso tiene password, agrega `spring-security-crypto` al `pom.xml`
  (sin versión; la gestiona Spring Boot) y sigue la sección 8 de `references/layers.md`.
- Para valores únicos repetidos (username, email, sku) lanza una
  `DuplicateResourceException` y tradúcela a 409 Conflict en el advice.

## 7. Ejecutar y verificar

Para iniciar la aplicación localmente usa siempre el wrapper:

```bash
./mvnw -DskipTests spring-boot:run
```

Para compilar sin levantar: `./mvnw -q -DskipTests compile`. Con JPA, la
aplicación necesita PostgreSQL disponible (ver `references/configuration.md`). Una vez
arriba: Swagger UI en `/swagger-ui.html`, health en `/actuator/health`.

Al terminar, resume al usuario la versión de Spring Boot usada, el package base
y los archivos creados.
