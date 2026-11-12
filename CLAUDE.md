# CLAUDE.md

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
