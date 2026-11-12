# Plantillas por capa

Ejemplo con el recurso `Product`. `{base}` es el package base
(`com.andres.course.claude.springboot.{artifactId}.app`). Adapta nombres y
campos al recurso real; mantén la forma.

Las plantillas de entity, repository y service muestran la variante **con JPA**.
Si el proyecto no tiene persistencia (sin `spring-boot-starter-data-jpa` en el
`pom.xml`), usa las variantes "sin JPA" de las secciones 1 y 4, y quita los
`@Transactional` del service.

## Contenido

1. Entity (`models`)
2. DTO record (`dto`)
3. Mapper (`mappers`)
4. Repository (`repositories`): con JPA y en memoria
5. Service: contrato + impl (`services`)
6. Controller REST (`controllers`)
7. Excepciones y advice (`exceptions`)
8. Recurso con campo sensible (`User` con password)

---

## 1. Entity — `models/Product.java`

```java
package {base}.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Product() {
    }

    // getters y setters de todos los campos
}
```

**Sin JPA**: mismo POJO sin anotaciones `jakarta.persistence` / Hibernate; el
repositorio en memoria asigna el `id` y los timestamps.

## 2. DTO — `dto/ProductDto.java`

Mismo record para request y response. Sin `createdAt` / `updatedAt` ni campos
sensibles.

```java
package {base}.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductDto(
        Long id,

        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 500)
        String description,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal price
) {
}
```

## 3. Mapper — `mappers/ProductMapper.java`

```java
package {base}.mappers;

import {base}.dto.ProductDto;
import {base}.models.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    /** entity -> dto */
    public ProductDto toDto(Product entity) {
        if (entity == null) {
            return null;
        }
        return new ProductDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice()
        );
    }

    /** dto -> entity (nueva; el id lo genera la base de datos) */
    public Product toEntity(ProductDto dto) {
        if (dto == null) {
            return null;
        }
        Product entity = new Product();
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setPrice(dto.price());
        return entity;
    }

    /** Copia los campos editables del dto sobre una entidad existente. */
    public void updateEntity(ProductDto dto, Product entity) {
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setPrice(dto.price());
    }
}
```

## 4. Repository — `repositories/ProductRepository.java`

```java
package {base}.repositories;

import {base}.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNameIgnoreCase(String name);
}
```

### Sin JPA: interfaz + implementación en memoria

`repositories/ProductRepository.java`:

```java
package {base}.repositories;

import {base}.models.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    List<Product> findAll();

    Optional<Product> findById(Long id);

    Product save(Product product);

    void delete(Product product);
}
```

`repositories/ProductRepositoryImpl.java`:

```java
package {base}.repositories;

import {base}.models.Product;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final Map<Long, Product> data = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public Product save(Product product) {
        LocalDateTime now = LocalDateTime.now();
        if (product.getId() == null) {
            product.setId(sequence.incrementAndGet());
            product.setCreatedAt(now);
        }
        product.setUpdatedAt(now);
        data.put(product.getId(), product);
        return product;
    }

    @Override
    public void delete(Product product) {
        data.remove(product.getId());
    }
}
```

Los nombres coinciden con los de `JpaRepository`, así que el service no cambia
si después se migra a JPA.

## 5. Service

### Contrato — `services/ProductService.java`

```java
package {base}.services;

import {base}.dto.ProductDto;

import java.util.List;

public interface ProductService {

    List<ProductDto> findAll();

    ProductDto findById(Long id);

    ProductDto create(ProductDto dto);

    ProductDto update(Long id, ProductDto dto);

    void delete(Long id);
}
```

### Implementación — `services/ProductServiceImpl.java`

```java
package {base}.services;

import {base}.dto.ProductDto;
import {base}.exceptions.ResourceNotFoundException;
import {base}.mappers.ProductMapper;
import {base}.models.Product;
import {base}.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> findAll() {
        return productRepository.findAll().stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto findById(Long id) {
        return productMapper.toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public ProductDto create(ProductDto dto) {
        Product saved = productRepository.save(productMapper.toEntity(dto));
        return productMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ProductDto update(Long id, ProductDto dto) {
        Product entity = getOrThrow(id);
        productMapper.updateEntity(dto, entity);
        return productMapper.toDto(productRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        productRepository.delete(getOrThrow(id));
    }

    private Product getOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
```

## 6. Controller REST — `controllers/ProductController.java`

```java
package {base}.controllers;

import {base}.dto.ProductDto;
import {base}.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductDto> findAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ProductDto findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@Valid @RequestBody ProductDto dto) {
        ProductDto created = productService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductDto dto) {
        return productService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## 7. Excepciones

### `exceptions/ResourceNotFoundException.java`

```java
package {base}.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " with id " + id + " not found");
    }
}
```

### `exceptions/GlobalExceptionHandler.java`

```java
package {base}.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }
}
```

El advice se acota con `annotations = RestController.class` (y no con
`basePackages`, que también incluiría el subpackage `controllers.web`) para que
los errores de las vistas Thymeleaf no se respondan como JSON.

## 8. Recurso con campo sensible (password)

Se sigue usando **un solo record** para request y response. El `password` va en
el record como campo **solo de escritura**: Jackson lo lee del JSON de entrada
pero nunca lo escribe en la respuesta. Así no hacen falta DTOs extra.

Reglas:
- `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` en el componente. En
  Spring Boot 4 (Jackson 3) la anotación sigue en `com.fasterxml.jackson.annotation`.
- Sin `@NotBlank`: en una actualización el password es opcional (si viene vacío
  se conserva el actual). Solo `@Size`; el service exige que venga al crear.
- El mapper **no** lo copia: `toDto` pone `null` y `toEntity` / `updateEntity`
  lo ignoran. El service lo hashea y lo asigna a la entidad.
- Para hashear agrega `org.springframework.security:spring-security-crypto` (la
  versión la gestiona Spring Boot; no trae Spring Security completo ni activa
  login) y expón un bean `PasswordEncoder` con `BCryptPasswordEncoder`.

### `dto/UserDto.java`

```java
package {base}.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserDto(
        Long id,

        @NotBlank @Size(max = 60)
        String username,

        @NotBlank @Email @Size(max = 120)
        String email,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Size(min = 8, max = 72)
        String password
) {
}
```

### `mappers/UserMapper.java` (extracto)

```java
public UserDto toDto(User entity) {
    return new UserDto(entity.getId(), entity.getUsername(), entity.getEmail(), null);
}

public User toEntity(UserDto dto) {
    User entity = new User();
    entity.setUsername(dto.username());
    entity.setEmail(dto.email());
    return entity;            // el password lo asigna el service, hasheado
}

public void updateEntity(UserDto dto, User entity) {
    entity.setUsername(dto.username());
    entity.setEmail(dto.email());
}
```

### `services/UserServiceImpl.java` (extracto)

```java
@Override
@Transactional
public UserDto create(UserDto dto) {
    if (dto.password() == null || dto.password().isBlank()) {
        throw new IllegalArgumentException("password is required");
    }
    User entity = userMapper.toEntity(dto);
    entity.setPassword(passwordEncoder.encode(dto.password()));
    return userMapper.toDto(userRepository.save(entity));
}

@Override
@Transactional
public UserDto update(Long id, UserDto dto) {
    User entity = getOrThrow(id);
    userMapper.updateEntity(dto, entity);
    if (dto.password() != null && !dto.password().isBlank()) {
        entity.setPassword(passwordEncoder.encode(dto.password()));
    }
    return userMapper.toDto(userRepository.save(entity));
}
```

Maneja `IllegalArgumentException` en el `GlobalExceptionHandler` como 400.

### `config/PasswordConfig.java`

```java
package {base}.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```
