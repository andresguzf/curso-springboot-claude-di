# Configuración: `application.properties`

Reemplaza `{artifactId}` por el artifactId real y `{db_name}` por un nombre de
base de datos derivado de él (snake_case, p. ej. `spring_di`). Las credenciales
vienen de variables de entorno; los valores por defecto son genéricos de
desarrollo local, nunca datos reales.

Base (siempre):

```properties
spring.application.name={artifactId}

# --- OpenAPI / Swagger UI ---
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html

# --- Actuator ---
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized

# --- Errores ---
spring.mvc.problemdetails.enabled=true
```

Solo con persistencia (JPA + PostgreSQL), agrega:

```properties
# --- PostgreSQL ---
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/{db_name}}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}

# --- JPA / Hibernate ---
# update es cómodo en desarrollo; en producción usar validate + migraciones (Flyway).
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
```

Para monolito con Thymeleaf agrega:

```properties
# Recarga de plantillas sin reiniciar (devtools)
spring.thymeleaf.cache=false
```

## PostgreSQL local (opcional)

Si el usuario no tiene PostgreSQL corriendo, sugiere un contenedor con las
mismas credenciales por defecto de desarrollo:

```bash
docker run --name {artifactId}-db -e POSTGRES_DB={db_name} -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:18
```

## Agregar JPA + PostgreSQL a un proyecto existente

Cuando el proyecto se creó sin persistencia y el usuario la pide después, agrega
al `pom.xml` (las versiones las gestiona el parent de Spring Boot):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
```

Luego: agrega las propiedades de PostgreSQL/JPA de arriba, anota los models con
`@Entity`, cambia cada repositorio a `extends JpaRepository<X, Long>`, elimina
las implementaciones en memoria y agrega `@Transactional` en los services.
Verifica con `./mvnw -q -DskipTests compile`.
