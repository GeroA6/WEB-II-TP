# TP2 · Persistencia, migraciones y arquitectura hexagonal

Resolución del Trabajo Práctico 2 para la materia **Web II**. El proyecto implementa una API REST con **Spring Boot 4.1.x**, **Java 25**, **PostgreSQL**, Spring Data JPA y Flyway. Aplica puertos y adaptadores para persistir favoritos, permite organizarlos en listas y documenta la API con Swagger / OpenAPI.

---

## Requisitos y cómo levantar el proyecto

Se requiere Java 25 y PostgreSQL en `localhost:5432`. Crear la base y el usuario una única vez:

```sql
CREATE USER webii_tp2 WITH PASSWORD 'webii_tp2';
CREATE DATABASE webii_tp2 OWNER webii_tp2;
```

La configuración se encuentra en `src/main/resources/application.properties`. Hibernate valida el esquema existente (`ddl-auto=validate`) y Flyway aplica las migraciones al iniciar la aplicación.

Usar siempre el wrapper de Maven (`mvnw`):

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

La documentación organiza los endpoints mediante `@Tag`:
- `productos`: Catálogo de solo lectura que consume una API externa.
- `favoritos`: Recurso propio persistido en PostgreSQL.
- `listas`: Recurso para agrupar favoritos y ejecutar el traslado transaccional.

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

### 3. Favoritos (CRUD persistido)
| Método | Path | Qué hace | Códigos HTTP |
|---|---|---|---|
| POST | `/api/favoritos` | Crea un favorito asociado a una lista | 201 Created (+ Location) / 400 Bad Request / 404 Not Found |
| GET | `/api/favoritos` | Lista todos los favoritos | 200 OK |
| GET | `/api/favoritos/{id}` | Obtiene un favorito por su ID | 200 OK / 404 Not Found |
| PUT | `/api/favoritos/{id}` | Actualiza un favorito (conserva `fechaAgregado`) | 200 OK / 400 Bad Request / 404 Not Found |
| DELETE | `/api/favoritos/{id}` | Elimina un favorito por su ID | 204 No Content / 404 Not Found |

### 4. Listas de favoritos
| Método | Path | Qué hace | Códigos HTTP |
|---|---|---|---|
| POST | `/api/listas` | Crea una lista | 201 Created (+ Location) / 400 Bad Request |
| GET | `/api/listas` | Lista todas las listas | 200 OK |
| GET | `/api/listas/{id}` | Obtiene una lista por su ID | 200 OK / 404 Not Found |
| GET | `/api/listas/{id}/favoritos` | Lista los favoritos asociados | 200 OK / 404 Not Found |
| DELETE | `/api/listas/{id}` | Elimina una lista vacía | 204 No Content / 404 Not Found / 409 Conflict |
| POST | `/api/listas/{origenId}/mover-favoritos` | Mueve los favoritos al destino y elimina el origen | 204 No Content / 400 Bad Request / 404 Not Found |

---

## Colección de Pruebas (`requests.http`)

En la raíz del proyecto se incluye el archivo `requests.http`, que contiene peticiones listas para ejecutar con la extensión **REST Client** de VS Code o **HTTP Client** de IntelliJ.

Incluye pruebas para:
- **Casos de éxito:**
  - Consumo y mapeo de productos.
  - CRUD de listas y creación de favoritos asociados a una lista.
  - Lectura, actualización y eliminación de favoritos.
  - Obtención de favoritos por lista y traslado transaccional entre listas.
- **Casos de error:**
  - `404 Not Found`: Búsqueda, actualización o eliminación de IDs inexistentes (en productos y favoritos).
  - `400 Bad Request`: Validación fallida al enviar campos en blanco o nulos en `FavoritoRequest`.
  - `409 Conflict`: Intento de eliminar una lista que todavía posee favoritos.

---

## Arquitectura y Decisiones de Diseño

1. **Desacoplamiento con DTOs (`records`):**
   - La API no expone el modelo tal cual lo devuelve DummyJSON (`DummyJsonProducto`), sino un DTO propio (`ProductoDTO`).
   - Para favoritos se separó la entrada (`FavoritoRequest`, con validaciones `@NotNull` y `@NotBlank`) de la salida (`FavoritoResponse`), protegiendo la inmutabilidad y evitando manipulación indebida de `id` o `fechaAgregado`.
2. **Puertos y adaptadores para favoritos:**
   - `FavoritoRepository` es el puerto: define el contrato que necesita el dominio para guardar, consultar, actualizar y eliminar favoritos.
   - `FavoritoService` depende de ese contrato, no de una tecnología de persistencia concreta. Por eso no cambió al reemplazar la implementación en memoria por `FavoritoRepositoryAdapter`.
   - El adapter JPA transforma entre `Favorito` (record inmutable del dominio) y `FavoritoEntity` (entidad mutable que Hibernate puede administrar). Así, los detalles de JPA y PostgreSQL permanecen fuera del dominio.
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
