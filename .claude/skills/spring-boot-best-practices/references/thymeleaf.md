# Vistas Thymeleaf + Tailwind CSS

Layout reutilizable con Thymeleaf puro (fragment expressions, sin dependencias
extra) y Tailwind CSS por CDN. HTML semántico: `header` (con `nav`), `main` con
el contenido centrado y `footer`.

```
src/main/resources/templates/
├── layouts/base.html
├── fragments/header.html
├── fragments/footer.html
├── index.html
└── products/
    ├── list.html
    └── form.html
```

## `layouts/base.html`

Recibe el `title` y el `content` de cada página.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org"
      th:fragment="layout(title, content)">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title th:replace="${title}">App</title>
    <script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>
</head>
<body class="min-h-screen flex flex-col bg-slate-50 text-slate-800">

<header th:replace="~{fragments/header :: header}"></header>

<main class="flex-1 w-full max-w-5xl mx-auto px-4 py-8">
    <th:block th:replace="${content}"></th:block>
</main>

<footer th:replace="~{fragments/footer :: footer}"></footer>

</body>
</html>
```

## `fragments/header.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<header th:fragment="header" class="bg-slate-900 text-white">
    <nav class="max-w-5xl mx-auto px-4 h-14 flex items-center justify-between">
        <a th:href="@{/}" class="font-semibold text-lg">App</a>
        <ul class="flex gap-6 text-sm">
            <li><a th:href="@{/}" class="hover:text-sky-300">Inicio</a></li>
            <li><a th:href="@{/products}" class="hover:text-sky-300">Productos</a></li>
        </ul>
    </nav>
</header>
</body>
</html>
```

## `fragments/footer.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<footer th:fragment="footer" class="border-t border-slate-200 bg-white">
    <div class="max-w-5xl mx-auto px-4 py-4 text-sm text-slate-500 text-center">
        &copy; <span th:text="${#temporals.year(#temporals.createNow())}">2026</span> App
    </div>
</footer>
</body>
</html>
```

## Página que usa el layout — `products/list.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layouts/base :: layout(~{::title}, ~{::section})}">
<head>
    <title>Productos</title>
</head>
<body>
<section>
    <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Productos</h1>
        <a th:href="@{/products/new}"
           class="rounded bg-sky-600 px-4 py-2 text-white hover:bg-sky-700">Nuevo</a>
    </div>

    <table class="w-full bg-white rounded shadow text-left">
        <thead class="bg-slate-100 text-sm">
        <tr><th class="p-3">Nombre</th><th class="p-3">Precio</th><th class="p-3"></th></tr>
        </thead>
        <tbody>
        <tr th:each="p : ${products}" class="border-t">
            <td class="p-3" th:text="${p.name}">Nombre</td>
            <td class="p-3" th:text="${p.price}">0</td>
            <td class="p-3 text-right">
                <a th:href="@{/products/{id}/edit(id=${p.id})}" class="text-sky-700 hover:underline">Editar</a>
            </td>
        </tr>
        </tbody>
    </table>
</section>
</body>
</html>
```

## Formulario con validación — `products/form.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layouts/base :: layout(~{::title}, ~{::section})}">
<head><title>Producto</title></head>
<body>
<section class="max-w-lg mx-auto">
    <h1 class="text-2xl font-bold mb-6" th:text="${product.id == null} ? 'Nuevo producto' : 'Editar producto'">Producto</h1>
    <form th:action="${product.id == null} ? @{/products} : @{/products/{id}(id=${product.id})}"
          th:object="${product}" method="post" class="space-y-4 bg-white p-6 rounded shadow">
        <div>
            <label for="name" class="block text-sm font-medium">Nombre</label>
            <input id="name" th:field="*{name}" class="mt-1 w-full rounded border px-3 py-2">
            <p th:if="${#fields.hasErrors('name')}" th:errors="*{name}" class="text-sm text-red-600"></p>
        </div>
        <div>
            <label for="price" class="block text-sm font-medium">Precio</label>
            <input id="price" type="number" step="0.01" th:field="*{price}" class="mt-1 w-full rounded border px-3 py-2">
            <p th:if="${#fields.hasErrors('price')}" th:errors="*{price}" class="text-sm text-red-600"></p>
        </div>
        <button type="submit" class="rounded bg-sky-600 px-4 py-2 text-white hover:bg-sky-700">Guardar</button>
    </form>
</section>
</body>
</html>
```

## Controller de vistas — `controllers/web/ProductPageController.java`

Usa el mismo `ProductService` y `ProductDto` que la API REST. Como el DTO es un
record (inmutable), el formulario nuevo se inicializa con un record vacío.

```java
package {base}.controllers.web;

import {base}.dto.ProductDto;
import {base}.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/products")
public class ProductPageController {

    private final ProductService productService;

    public ProductPageController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        return "products/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new ProductDto(null, "", "", null));
        return "products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") ProductDto product, BindingResult result) {
        if (result.hasErrors()) {
            return "products/form";
        }
        productService.create(product);
        return "redirect:/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        return "products/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("product") ProductDto product,
                         BindingResult result) {
        if (result.hasErrors()) {
            return "products/form";
        }
        productService.update(id, product);
        return "redirect:/products";
    }
}
```

## Página de inicio — `controllers/web/HomeController.java`

```java
package {base}.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
```
