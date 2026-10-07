# TP1 · Spring Boot, API REST y arquitectura en capas

Resolución del Trabajo Práctico 1 para la materia **Web II**.  
El proyecto implementa una API REST modular construida sobre **Spring Boot 4.1.x** y **Java 25**, aplicando una arquitectura en capas (**Controller → Service → Repository / Client**), desacoplamiento mediante DTOs, validación con Bean Validation, manejo centralizado de errores (`ProblemDetail` RFC 7807) y documentación con Swagger / OpenAPI.

---

## Cómo levantar el proyecto

Requiere Java 25. Usar siempre el wrapper de Maven (`mvnw`):

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

Cuando el log muestre `Started DemoApplication`, la API quedará escuchando en `http://localhost:8080`.

Para compilar y correr los tests:

```bash
# Windows
.\mvnw.cmd test

# macOS / Linux
./mvnw test
```

---

## Documentación interactiva (Swagger UI)

Una vez levantada la aplicación, podés consultar y probar todos los endpoints de forma interactiva en Swagger UI navegando a:

**[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**  
*(o alternativamente `http://localhost:8080/swagger-ui.html`)*

La documentación organiza los endpoints en dos grupos principales mediante `@Tag`:
- `productos`: Catálogo de solo lectura que consume una API externa.
- `favoritos`: Recurso propio con CRUD completo en memoria.

---

## Endpoints de la API

### 1. Utilidades y Health Check
| Método | Path | Qué hace | Código HTTP |
|---|---|---|---|
| GET | `/health` | Chequeo de salud del servicio | 200 OK |
| GET | `/ping` | Chequeo trivial (devuelve `pong`) | 200 OK |

### 2. Catálogo de Productos (Consumo externo a DummyJSON)
| Método | Path | Qué hace | Códigos HTTP |
|---|---|---|---|
| GET | `/api/productos` | Lista todos los productos mapeados a `ProductoDTO` | 200 OK |
| GET | `/api/productos/{id}` | Obtiene un producto por su ID | 200 OK / 404 Not Found |

### 3. Favoritos (CRUD propio en memoria)
| Método | Path | Qué hace | Códigos HTTP |
|---|---|---|---|
| POST | `/api/favoritos` | Crea un favorito (valida campos obligatorios) | 201 Created (+ Location) / 400 Bad Request |
| GET | `/api/favoritos` | Lista todos los favoritos | 200 OK |
| GET | `/api/favoritos/{id}` | Obtiene un favorito por su ID | 200 OK / 404 Not Found |
| PUT | `/api/favoritos/{id}` | Actualiza un favorito (conserva `fechaAgregado`) | 200 OK / 400 Bad Request / 404 Not Found |
| DELETE | `/api/favoritos/{id}` | Elimina un favorito por su ID | 204 No Content / 404 Not Found |

---

## Colección de Pruebas (`requests.http`)

En la raíz del proyecto se incluye el archivo `requests.http`, que contiene peticiones listas para ejecutar con la extensión **REST Client** de VS Code o **HTTP Client** de IntelliJ.

Incluye pruebas para:
- **Casos de éxito:**
  - Consumo y mapeo de productos.
  - Creación de favorito con retorno de `201 Created` y cabecera `Location`.
  - Lectura, actualización y eliminación de favoritos.
- **Casos de error:**
  - `404 Not Found`: Búsqueda, actualización o eliminación de IDs inexistentes (en productos y favoritos).
  - `400 Bad Request`: Validación fallida al enviar campos en blanco o nulos en `FavoritoRequest`.

---

## Arquitectura y Decisiones de Diseño

1. **Desacoplamiento con DTOs (`records`):**
   - La API no expone el modelo tal cual lo devuelve DummyJSON (`DummyJsonProducto`), sino un DTO propio (`ProductoDTO`).
   - Para favoritos se separó la entrada (`FavoritoRequest`, con validaciones `@NotNull` y `@NotBlank`) de la salida (`FavoritoResponse`), protegiendo la inmutabilidad y evitando manipulación indebida de `id` o `fechaAgregado`.
2. **Repositorio en Memoria Thread-Safe:**
   - La implementación `InMemoryFavoritoRepository` utiliza `ConcurrentHashMap` y `AtomicLong` para garantizar consistencia ante múltiples peticiones concurrentes en Tomcat.
3. **Manejo Centralizado de Excepciones:**
   - Mediante `@RestControllerAdvice` (`GlobalExceptionHandler`), todos los errores se traducen a respuestas estándar bajo la especificación **ProblemDetail (RFC 7807)** con código de estado, título y detalle.

---

## Persistencia y Evolución del Esquema (Flyway)

### ¿Por qué se resolvió con una nueva migración (V4) y no modificando V3?
En la Consigna 6, la conversión de `lista_id` en una columna obligatoria (`NOT NULL`) y el saneamiento de datos existentes se implementaron creando la migración `V4__lista_id_obligatorio.sql` en lugar de alterar `V3__add_lista_id_a_favoritos.sql`. Esto responde a principios fundamentales de gestión de bases de datos en producción:

1. **Inmutabilidad y validación de Checksums en Flyway:**
   Flyway almacena en la tabla de metadatos `flyway_schema_history` una suma de comprobación (checksum) por cada script ejecutado. Si modificamos un archivo ya aplicado como `V3`, Flyway detectará una disparidad en el checksum al iniciar la aplicación y abortará el arranque con una excepción (`FlywayValidateException` / `MigrationChecksumException`).
2. **Consistencia entre múltiples entornos (Local, CI/CD, Staging, Producción):**
   En un flujo de trabajo profesional, `V3` ya pudo haberse corrido en bases de datos de otros desarrolladores o en ambientes compartidos. Flyway no vuelve a ejecutar scripts con números de versión pasados; por lo tanto, modificar `V3` dejaría a los entornos previamente migrados sin la restricción `NOT NULL`, rompiendo la paridad.
3. **Evolución segura con migración de datos (Backfill):**
   Las migraciones reflejan el historial cronológico del esquema. Para pasar de una columna opcional a una obligatoria en una base de datos con datos reales, es indispensable una estrategia en fases:
   - Primero se creó como anulable en `V3`.
   - Luego, en `V4`, se crearon los valores por defecto (`'Sin clasificar'`) y se actualizaron los registros huérfanos antes de imponer la restricción física `ALTER COLUMN lista_id SET NOT NULL`. Modificar `V3` directamente habría impedido este saneamiento progresivo en bases ya pobladas.

---

## Transacciones y atomicidad: mover favoritos entre listas

El endpoint `POST /api/listas/{origenId}/mover-favoritos` traslada todos los favoritos de una lista de origen hacia otra lista de destino. La petición recibe el ID de destino:

```json
{
  "destinoId": 2
}
```

El caso de uso se resuelve en `ListaService`, que es la capa que contiene las reglas de negocio. Antes de modificar datos, valida que las listas de origen y destino existan; si alguna no existe, la API responde `404 Not Found`. También rechaza usar la misma lista como origen y destino con `400 Bad Request`.

Una vez validados los datos, el servicio reasigna cada `FavoritoEntity` a la lista destino y elimina la lista origen. El método está anotado con `@Transactional`, por lo que todos esos pasos conforman una única transacción de base de datos.

### ¿Por qué es necesaria la transacción?

La operación requiere preservar especialmente dos propiedades ACID:

1. **Atomicidad:** el traslado completo se confirma o se revierte por completo. Si falla el guardado de los favoritos o la eliminación de la lista de origen, Spring marca la transacción para rollback y PostgreSQL no conserva cambios parciales.
2. **Consistencia:** al finalizar exitosamente, todos los favoritos que pertenecían al origen apuntan al destino y la lista origen ya no existe. No queda una lista eliminada con favoritos que la referencien, ni favoritos en un estado intermedio.

Sin `@Transactional`, cada operación podría confirmarse por separado. Por ejemplo, se podrían mover los favoritos pero fallar al borrar la lista de origen; ese resultado no representa el caso de uso solicitado y deja la información parcialmente actualizada.
