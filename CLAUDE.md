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
│   ├── models/                         # vacío (package-info.java)
│   ├── dto/                            # vacío (package-info.java)
│   ├── mappers/                        # vacío (package-info.java)
│   ├── repositories/                   # vacío (package-info.java)
│   ├── services/                       # vacío (package-info.java)
│   ├── controllers/                    # vacío (package-info.java)
│   └── exceptions/
│       ├── ResourceNotFoundException.java
│       └── GlobalExceptionHandler.java # @RestControllerAdvice -> ProblemDetail (404, 400)
├── src/main/resources/application.properties   # springdoc, actuator, problemdetails
├── src/test/java/.../springdi/app/ApplicationTests.java
└── .claude/skills/spring-boot-best-practices/  # skill del proyecto
```

La clase principal se llama `Application.java` (y no `SpringDiApplication`)
porque Initializr no genera un nombre de clase a partir de un artifactId que
empieza con número. No la renombres.

Aún no hay recursos (entity, DTO, service, controller). Al agregar uno, crea el
conjunto completo siguiendo la skill y reemplaza el `package-info.java` solo si
el package queda con clases.

## Comandos

- Ejecutar: `./mvnw -DskipTests spring-boot:run`
- Compilar: `./mvnw -q -DskipTests compile`
- Tests: `./mvnw test`
- Swagger UI: http://localhost:8080/swagger-ui.html · Health: http://localhost:8080/actuator/health

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
