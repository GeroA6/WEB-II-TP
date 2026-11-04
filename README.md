# TP3 · Seguridad, pruebas e integración continua

[![CI - Integración Continua](https://github.com/GeroA6/WEB-II-TP/actions/workflows/ci.yml/badge.svg?branch=TP3)](https://github.com/GeroA6/WEB-II-TP/actions/workflows/ci.yml)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=GeroA6_WEB-II-TP&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=GeroA6_WEB-II-TP&branch=TP3)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=GeroA6_WEB-II-TP&metric=coverage)](https://sonarcloud.io/summary/new_code?id=GeroA6_WEB-II-TP&branch=TP3)

Resolución del Trabajo Práctico 3 para la materia **Web II** (UNVIME). El proyecto evoluciona la API REST del TP2 incorporando autenticación sin estado mediante **JWT**, control de acceso y autorización fina por **propiedad de recursos** y jerarquía de roles (`USER` y `ADMIN`), suite de pruebas automatizadas con **Mockito** y **Testcontainers**, pipeline de **CI en GitHub Actions**, dockerización multi-etapa y despliegue en la nube en **Render**.

---

## Enlaces Rápidos y Accesos Públicos

* **Despliegue en Producción (Render):** [https://webii-tp3-app.onrender.com/](https://webii-tp3-app.onrender.com/)
* **Health Check en Vivo:** [https://webii-tp3-app.onrender.com/health](https://webii-tp3-app.onrender.com/health)
* **Documentación Interactiva (Swagger UI):** [https://webii-tp3-app.onrender.com/swagger-ui/index.html](https://webii-tp3-app.onrender.com/swagger-ui/index.html)
* **SonarCloud Dashboard (Rama TP3):** [https://sonarcloud.io/summary/new_code?id=GeroA6_WEB-II-TP&branch=TP3](https://sonarcloud.io/summary/new_code?id=GeroA6_WEB-II-TP&branch=TP3)
* **SonarCloud Overview (Métricas completas):** [https://sonarcloud.io/project/overview?id=GeroA6_WEB-II-TP&branch=TP3](https://sonarcloud.io/project/overview?id=GeroA6_WEB-II-TP&branch=TP3)

---

## Despliegue en Producción (Render)

La aplicación se encuentra desplegada y operativa en **Render** conectada mediante red interna privada a una base de datos PostgreSQL gestionada:

* **URL Pública de la API:** [https://webii-tp3-app.onrender.com/](https://webii-tp3-app.onrender.com/)
* **Health Check:** [https://webii-tp3-app.onrender.com/health](https://webii-tp3-app.onrender.com/health)
* **Documentación interactiva (Swagger UI):** [https://webii-tp3-app.onrender.com/swagger-ui/index.html](https://webii-tp3-app.onrender.com/swagger-ui/index.html)

### Evidencia del Despliegue en Render

A continuación se presentan las capturas organizadas por flujo de verificación (almacenadas en la carpeta [`Evidencia/`](./Evidencia)):

#### 1. Verificación de Salud (`GET /health`) y Documentación Swagger
| Health Check (`200 OK - UP`) | Swagger UI en Producción |
|:---:|:---:|
| ![Health Check](./Evidencia/Captura%20de%20pantalla%20health.png) | ![Swagger UI](./Evidencia/Captura%20de%20pantalla%20swagger.png) |

#### 2. Registro de Usuario (`POST /api/auth/registro`)
| Request de Registro | Respuesta `201 Created` con hash BCrypt |
|:---:|:---:|
| ![Registro Request](./Evidencia/Captura%20de%20pantalla%20registrar1.png) | ![Registro Response](./Evidencia/Captura%20de%20pantalla%20registrar2.png) |

#### 3. Autenticación y Emisión de JWT (`POST /api/auth/login`)
| Request de Login | Token JWT generado (`200 OK`) |
|:---:|:---:|
| ![Login Request](./Evidencia/Captura%20de%20pantalla%20login1.png) | ![Login Response](./Evidencia/Captura%20de%20pantalla%20login2.png) |

#### 4. Control de Acceso: Petición sin token vs Petición Autorizada
| Acceso Denegado sin Token (`401 Unauthorized`) | Acceso Concedido con Bearer JWT (`200 OK`) |
|:---:|:---:|
| ![Get Listas sin token 401](./Evidencia/Captura%20de%20pantalla%20get-listas.png) | ![Get Listas con token 200](./Evidencia/Captura%20de%20pantalla%20listas%20autorizado1.png) |

| Detalle de Respuesta con Token JWT |
|:---:|
| ![Detalle de Listas Autorizado](./Evidencia/Captura%20de%20pantalla%20listas%20autorizado2.png) |

---

## Cómo ejecutar las pruebas automatizadas

El proyecto implementa la pirámide de pruebas separando pruebas unitarias de pruebas de integración según la convención estándar de Maven:

### 1. Pruebas Unitarias (Surefire + Mockito + JUnit 5)
Se ejecutan en memoria de forma aislada, sin levantar Spring Boot ni requerir bases de datos externas:
* `UsuarioServiceImplTest`: Verifica que la contraseña **nunca se guarde en texto plano** (comprobando el hash de BCrypt mediante `ArgumentCaptor`) y la gestión de emails duplicados.
* `ListaServiceTest`: Verifica que al crear una lista quede **asociada al ID del usuario autenticado** y valida el control de acceso de dueños vs rol `ADMIN`.

Comando para correrlas:
```bash
# Windows
.\mvnw test

# Linux / macOS
./mvnw test
```

### 2. Pruebas de Integración (Failsafe + Testcontainers + PostgreSQL real)
Ubicadas en clases `*IT.java` (`AuthYAutorizacionIT`). Testcontainers levanta un contenedor efímero de PostgreSQL en Docker y Flyway aplica las migraciones `V1` a `V6` desde cero:
* Login válido $\rightarrow$ `200 OK` con token JWT.
* Login inválido $\rightarrow$ `401 Unauthorized`.
* Acceso a lista propia $\rightarrow$ `200 OK`.
* Acceso a lista ajena $\rightarrow$ `403 Forbidden`.
* Acceso sin token $\rightarrow$ `401 Unauthorized` (vía `HttpStatusEntryPoint`).
* Usuario `ADMIN` accediendo a lista ajena $\rightarrow$ `200 OK`.

Comando para correr la suite completa (unitarias + integración):
```bash
# Windows
.\mvnw verify

# Linux / macOS
./mvnw verify
```

### 3. Reporte de Cobertura con JaCoCo
El plugin `jacoco-maven-plugin` (versión 0.8.14 compatible con Java 25) genera métricas de cobertura al ejecutar la fase de verificación:
* **Reporte HTML interactivo local:** generado en `target/site/jacoco/index.html`.
* Para abrirlo en el navegador:
  ```powershell
  # Windows
  start target\site\jacoco\index.html
  ```
* **Integración con SonarCloud:** genera además `target/site/jacoco/jacoco.xml`, el cual es consumido por el pipeline de CI para alimentar el dashboard en la nube.

---

## Integración Continua (CI con GitHub Actions)

El archivo [`.github/workflows/ci.yml`](./.github/workflows/ci.yml) automatiza el ciclo de integración ante cada `push` o `pull request` en las ramas `main` y `TP3`:
1. Levanta un entorno limpio con Ubuntu y **Java 25**.
2. Garantiza permisos de ejecución multiplataforma para `mvnw`.
3. Ejecuta `./mvnw -B verify`: compila el proyecto y corre tanto las pruebas unitarias como las de integración contra Docker.

---

## Ejecución Local con Docker Compose

Para levantar todo el stack (Spring Boot + PostgreSQL) sin necesidad de tener Java ni PostgreSQL instalados en la máquina anfitriona:

```bash
docker compose up --build
```
* La aplicación quedará escuchando en `http://localhost:8080`.
* La base de datos persistirá sus datos en el volumen administrado `pgdata`.

---

## Arquitectura y Decisiones de Diseño

### ¿Por qué la autorización por propiedad vive en el Service y no en el Controller? (Consigna 4.5)

El control de acceso por recurso (`buscarPropiaOFallar` en `ListaService`) se implementó deliberadamente en la capa de **Servicio** por las siguientes razones de arquitectura:

1. **Regla de negocio del dominio:** La pertenencia de un recurso forma parte de las invariantes de negocio, no del protocolo HTTP ni de los encabezados de red.
2. **Acceso al estado persistido:** Para saber si una lista le pertenece al usuario autenticado, es indispensable cargar la entidad desde la base de datos. Los filtros web y controladores solo conocen el ID que viene en la URL y los claims del token, pero no poseen la entidad en memoria.
3. **Eficiencia y atomicidad:** Resolverlo en el `Service` evita consultas duplicadas. En una única llamada atómica se recupera la entidad, se valida que exista (o 404), se comprueba la propiedad (o 403) y se procede con la lógica de negocio.
4. **Reutilización:** Si en el futuro se añade otro canal de entrada (gRPC, mensajería asíncrona, tareas programadas), las reglas de seguridad siguen vigentes sin depender de la capa web.

---

## Endpoints de la API y Seguridad

| Método | Path | Descripción | Seguridad | Códigos HTTP |
|---|---|---|---|---|
| **GET** | `/health` | Chequeo de salud del servicio | Público | 200 |
| **GET** | `/ping` | Chequeo trivial | Público | 200 |
| **GET** | `/api/productos/**` | Catálogo externo DummyJSON | Público | 200, 404 |
| **POST** | `/api/auth/registro` | Registro con contraseña hasheada (BCrypt) | Público | 201, 400, 409 |
| **POST** | `/api/auth/login` | Autenticación y emisión de JWT | Público | 200, 401 |
| **GET** | `/api/listas` | Listar listas propias (o todas si es ADMIN) | **Bearer JWT** | 200, 401 |
| **POST** | `/api/listas` | Crear lista (dueño automático vía token) | **Bearer JWT** | 201, 400, 401 |
| **GET** | `/api/listas/{id}` | Obtener lista propia o si es ADMIN | **Bearer JWT** | 200, 401, 403, 404 |
| **DELETE** | `/api/listas/{id}` | Eliminar lista propia o si es ADMIN | **Bearer JWT** | 204, 401, 403, 404, 409 |
| **POST** | `/api/listas/{id}/mover-favoritos` | Mover favoritos transaccionalmente | **Bearer JWT** | 204, 400, 401, 403, 404 |
| **GET** | `/api/favoritos` | Listar favoritos | **Bearer JWT** | 200, 401 |
| **POST** | `/api/favoritos` | Crear favorito | **Bearer JWT** | 201, 400, 401, 404 |
| **GET** | `/api/favoritos/{id}` | Obtener favorito por ID | **Bearer JWT** | 200, 401, 404 |
| **PUT** | `/api/favoritos/{id}` | Actualizar favorito | **Bearer JWT** | 200, 400, 401, 404 |
| **DELETE** | `/api/favoritos/{id}` | Eliminar favorito | **Bearer JWT** | 204, 401, 404 |

---

## Colección de Pruebas (`requests.http`)

En la raíz del proyecto se incluye el archivo `requests.http` con peticiones configuradas para probar de punta a punta:
* Registro y login obteniendo el token Bearer.
* Consultas públicas a productos y health.
* Acceso con y sin token.
* Operaciones CRUD sobre listas y favoritos.
