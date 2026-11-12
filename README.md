# 2-spring-di

Proyecto del curso de **Spring Boot con Claude Code**. Incluye la skill
`spring-boot-best-practices`, que permite a Claude Code generar y mantener
proyectos Spring Boot con una arquitectura en capas y convenciones comunes para
todo el curso.

> El proyecto Spring Boot todavía no está generado. Se crea pidiéndoselo a
> Claude Code (ver [Uso](#uso)), y la skill lo genera en este mismo directorio.

## Contenido

- [Stack](#stack)
- [Requisitos](#requisitos)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Uso](#uso)
- [Arquitectura del proyecto generado](#arquitectura-del-proyecto-generado)
- [Persistencia bajo demanda](#persistencia-bajo-demanda)
- [Vistas web (monolito)](#vistas-web-monolito)
- [Ejecutar la aplicación](#ejecutar-la-aplicación)
- [Endpoints útiles](#endpoints-útiles)
- [Pruebas de la skill](#pruebas-de-la-skill)

## Stack

| | |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1.2 o superior (última versión estable de start.spring.io) |
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
├── CLAUDE.md                                  # skills disponibles y reglas de activación
├── README.md
└── .claude/skills/
    ├── spring-boot-best-practices/
    │   ├── SKILL.md                           # instrucciones de la skill
    │   ├── scripts/create_project.sh          # genera el proyecto con Spring Initializr
    │   ├── references/
    │   │   ├── layers.md                      # plantillas: entity, DTO, mapper, repository, service, controller
    │   │   ├── configuration.md               # application.properties y PostgreSQL
    │   │   └── thymeleaf.md                   # layout, fragmentos y vistas con Tailwind
    │   └── evals/evals.json                   # casos de prueba de la skill
    └── spring-boot-best-practices-workspace/  # resultados de las pruebas (no es parte de la skill)
```

## Uso

Abre Claude Code en este directorio y pídele lo que necesites en lenguaje
natural. La skill se activa sola cuando la petición encaja con sus reglas
(definidas en [CLAUDE.md](CLAUDE.md)).

**Crear el proyecto:**

```text
Crea una API REST básica con Spring Boot para gestionar productos con nombre, descripción y precio.
```

```text
Quiero un monolito con Spring Web para gestionar tareas con páginas para listar y crear.
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

## Arquitectura del proyecto generado

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
| http://localhost:8080/api/{recurso} | Endpoints REST de cada recurso |

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
