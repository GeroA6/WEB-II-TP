# TP2 · Persistencia, Migraciones y Arquitectura Hexagonal

Guía de referencia rápida y requerimientos detallados del Trabajo Práctico 2 de **Web II**.

---

## 📌 Objetivos del TP2

1. Conectar Spring Boot a una base de datos relacional **PostgreSQL** mediante **Spring Data JPA**.
2. Aplicar el patrón de **Arquitectura Hexagonal (Puertos y Adaptadores)**: reemplazar el repositorio en memoria de favoritos por un adaptador JPA sin modificar el dominio (`Favorito`), el servicio (`FavoritoService`) ni el controlador (`FavoritoController`).
3. Versionar la evolución de la base de datos con **Flyway** (`db/migration`).
4. Modelar relaciones entre entidades (**Listas** y **Favoritos**, relación `@ManyToOne` unidireccional).
5. Implementar una operación transaccional atómica con `@Transactional` (mover favoritos entre listas y eliminar la de origen).
6. Documentar las decisiones de diseño y fundamentos teóricos en el `README.md`.

---

## 🏛️ Dominio del Práctico

- **Catálogo de productos (`/api/productos`):** No cambia. Sigue siendo de solo lectura consumiendo DummyJSON.
- **Favoritos (`/api/favoritos`):** Pasa de vivir en memoria (`ConcurrentHashMap`) a persistir en PostgreSQL.
- **Listas (`/api/listas`):** Nuevo recurso propio para agrupar favoritos (ej. "Regalos", "Ofertas"). Cada favorito pertenecerá a una lista.

---

## 📋 Las 8 Consignas Detalladas

### Consigna 1: PostgreSQL y dependencias
- **Base de datos:** Instancia de PostgreSQL corriendo en `localhost:5432` con base `webii_tp2`, usuario `webii_tp2`, contraseña `webii_tp2`.
- **Dependencias en `pom.xml`:**
  - `spring-boot-starter-data-jpa`
  - `org.postgresql:postgresql` (scope `runtime`)
  - `spring-boot-starter-flyway`
  - `flyway-database-postgresql`
  *(Nota: En Spring Boot 4.x se requiere el starter específico de Flyway para autoconfiguración).*
- **Configuración en `application.properties`:**
  ```properties
  spring.datasource.url=jdbc:postgresql://localhost:5432/webii_tp2
  spring.datasource.username=webii_tp2
  spring.datasource.password=webii_tp2

  # Flyway administra el esquema, NO Hibernate
  spring.jpa.hibernate.ddl-auto=validate
  spring.jpa.open-in-view=false
  ```

---

### Consigna 2: Migraciones iniciales con Flyway
- Crear migración SQL en `src/main/resources/db/migration/V1__create_favoritos.sql`.
- Tabla `favoritos`:
  - `id` (BIGSERIAL / BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY)
  - `producto_id` (BIGINT NOT NULL)
  - `nota` (TEXT NOT NULL)
  - `fecha_alta` (TIMESTAMP NOT NULL)
- Al iniciar la app, verificar en logs y en la base que Flyway creó `flyway_schema_history` y aplicó la migración.
- Regla: Una migración ya aplicada **nunca se modifica**.

---

### Consigna 3: Favoritos sobre JPA — El Adapter
- **`FavoritoEntity`** (paquete `infrastructure` o `entity`):
  - Anotada con `@Entity` y `@Table(name = "favoritos")`.
  - Clase mutable con constructor vacío y getters/setters (Hibernate lo requiere).
  - El record inmutable de dominio `Favorito` (TP1) **no se toca**.
- **`FavoritoJpaRepository`**:
  - Interfaz que extiende `JpaRepository<FavoritoEntity, Long>`.
- **`FavoritoRepositoryAdapter`**:
  - Clase con `@Repository` que implementa la interfaz del puerto `FavoritoRepository` (definida en el TP1).
  - Inyecta `FavoritoJpaRepository`.
  - Mapea internamente `FavoritoEntity` ↔ `Favorito` (record de dominio).
- **Eliminar `InMemoryFavoritoRepository`:**
  - Se quita para que Spring inyecte el nuevo adapter sin ambigüedad.
- Probar el CRUD y verificar que los datos persisten tras reiniciar la app.

---

### Consigna 4: Puertos y Adaptadores — Qué cambió y qué no
- **Verificación:** `FavoritoService`, `FavoritoController` y los DTOs de favoritos no deben tener ningún cambio respecto del TP1.
- **Justificación en el README:** Explicar cómo el puerto `FavoritoRepository` actuó como contrato invariable y los repositorios fueron adaptadores intercambiables.

---

### Consigna 5: Relación — Listas de Favoritos
1. **Dominio y Puerto:**
   - Dominio `Lista` (record: `Long id`, `String nombre`).
   - Puerto `ListaRepository` (interfaz con `findAll`, `findById`, `save`, `deleteById`).
2. **Infraestructura de Listas:**
   - `ListaEntity` (`@Entity`, tabla `listas`).
   - `ListaJpaRepository extends JpaRepository<ListaEntity, Long>`.
   - `ListaRepositoryAdapter implements ListaRepository`.
3. **Relación Favorito → Lista:**
   - En `FavoritoEntity`: agregar `@ManyToOne(fetch = FetchType.LAZY)` sobre `ListaEntity lista`.
   - En `FavoritoJpaRepository`: consulta derivada `List<FavoritoEntity> findByListaId(Long listaId)` (evita `@OneToMany` bidireccional complejo).
   - En el dominio `Favorito`, en `FavoritoRequest` (`@NotNull`) y en `FavoritoResponse`: sumar `Long listaId`.
4. **Migraciones:**
   - `V2__create_listas.sql`: Tabla `listas` (`id`, `nombre`).
   - `V3__add_lista_id_a_favoritos.sql`: Columna `lista_id BIGINT REFERENCES listas(id)` en `favoritos`.
5. **Endpoints de Listas:**
   - `POST /api/listas` (201 Created + header Location)
   - `GET /api/listas` (200 OK)
   - `GET /api/listas/{id}` (200 OK / 404 Not Found)
   - `GET /api/listas/{id}/favoritos` (200 OK - lista de favoritos que pertenecen a esa lista)
   - `DELETE /api/listas/{id}` (204 No Content / 409 Conflict si la lista aún tiene favoritos asociados).
6. **Manejo de Error 409:**
   - Capturar cuando se intente borrar una lista con favoritos y retornar `409 Conflict` (en lugar de un 500).

---

### Consigna 6: Evolución del Esquema
- **Problema:** Filas existentes creadas antes de la relación tienen `lista_id = NULL`.
- **Solución con Flyway:**
  - Crear `V4__lista_id_obligatorio.sql` que en este orden estricto:
    1. Inserte una lista por defecto ("Sin clasificar") si no existe.
    2. Actualice todos los favoritos con `lista_id IS NULL` asignándoles el ID de esa lista por defecto.
    3. Altere la columna `lista_id` para que sea `NOT NULL`.
- **Justificación en README:** Explicar por qué esto se resuelve con una migración nueva y no modificando las anteriores.

---

### Consigna 7: Transacciones y Atomicidad
- **Endpoint:** `POST /api/listas/{origenId}/mover-favoritos`
  - Body de entrada: ID de la lista destino (`MoverFavoritosRequest` con `Long destinoId`).
  - Lógica:
    1. Validar que lista origen y destino existan (404 si alguna no existe).
    2. Reasignar todos los favoritos de la lista origen a la lista destino.
    3. Eliminar la lista origen.
  - Anotar el método en el Service con `@Transactional`.
- **Justificación en README:** Explicar según las propiedades **ACID** (especialmente Atomicidad y Consistencia) qué pasaría si no estuviera `@Transactional` y una de las operaciones fallara a mitad de camino.

---

### Consigna 8: Documentación y Evidencia Esperada
- Documentar todos los nuevos endpoints en Swagger UI con `@Tag(name = "listas")` y `@Operation`.
- Actualizar `README.md` con:
  - Instrucciones de PostgreSQL.
  - Tabla completa de endpoints (Productos, Favoritos y Listas).
  - Justificación de Puertos y Adaptadores (Consigna 4).
  - Justificación de evolución del esquema con Flyway (Consigna 6).
  - Justificación de `@Transactional` y ACID (Consigna 7).
- Actualizar `requests.http` con pruebas de:
  - CRUD de listas.
  - Obtención de favoritos de una lista (`GET /api/listas/{id}/favoritos`).
  - Error 409 Conflict al borrar lista con favoritos.
  - Operación transaccional de mover favoritos (`POST /api/listas/{origenId}/mover-favoritos`).
