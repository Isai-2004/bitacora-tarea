# Bitácora API

API REST en **C# / ASP.NET Core 8** para administrar los registros de una bitácora. Está organizada por capas según la arquitectura limpia, usa **Entity Framework Core con SQLite**, está protegida con **JWT y autorización por roles**, se documenta con **OpenAPI / Swagger UI** y se prueba con **Postman**.

## Estructura de la solución

```
Bitacora.sln
├── src/
│   ├── Bitacora.Domain           Entidades Registro y Usuario, roles y reglas de negocio (no depende de nadie)
│   ├── Bitacora.Application      DTO, interfaces (repositorios, hash, token) y casos de uso (RegistroService, AuthService)
│   ├── Bitacora.Infrastructure   EF Core + SQLite, repositorios, migraciones, PasswordHasher y emisión de JWT
│   └── Bitacora.Api              Controladores, JwtBearer, manejador global de errores y Swagger
└── postman/
    ├── Bitacora.postman_collection.json     Colección con las pruebas de seguridad
    └── Bitacora.postman_environment.json    Entorno (sin secretos)
```

Dependencias (siempre apuntan hacia el dominio; ver también `docs/diagrama_capas.png`):

```
Api ──► Application ──► Domain
 │            ▲
 └──► Infrastructure ──┘
```

## Seguridad

| Tema | Implementación |
|---|---|
| Contraseñas | `PasswordHasher` de ASP.NET Core Identity (PBKDF2 + HMAC-SHA512, sal aleatoria, 100 000 iteraciones). Nunca se guardan en texto plano. |
| Registro | `POST /api/auth/registro` crea cuentas con el rol **Usuario**. El Admin inicial se crea desde la configuración secreta. |
| Login | `POST /api/auth/login` devuelve `token`, `expira` (UTC) y `rol`. Si falla responde **401** con el mismo mensaje para correo o contraseña incorrectos. |
| JWT | HMAC-SHA256. Se validan **emisor, audiencia, firma y tiempo de vida** (`ClockSkew = 0`). Vence en **10 minutos** (`Jwt:MinutosExpiracion`). |
| Clave de firma | **No está en el repositorio**: se guarda con `dotnet user-secrets` (o la variable de entorno `Jwt__Clave`). La API no arranca si falta. |
| Autorización | `[Authorize]` en todos los endpoints de registros, `[Authorize(Roles = "Admin")]` en DELETE y `[AllowAnonymous]` en login y registro. |
| Dueño | Cada registro guarda `UsuarioId`. Un Usuario solo modifica los suyos (si no, **403**); el Admin puede modificar cualquiera. |
| 401 vs 403 | 401 = no autenticado (sin token, alterado o vencido). 403 = autenticado pero sin permiso. Ambos con ProblemDetails. |
| HTTPS | `UseHttpsRedirection` activo. Swagger documenta el esquema **Bearer** (botón *Authorize*). |

## Requisitos

- [.NET SDK 8](https://dotnet.microsoft.com/download/dotnet/8.0)
- Herramienta de EF Core: `dotnet tool install --global dotnet-ef --version 8.0.11`
- Postman

## Cómo ejecutar

```powershell
# 1. Compilar
dotnet build

# 2. Configurar los secretos de ESTE equipo (clave JWT aleatoria + administrador inicial)
powershell -ExecutionPolicy Bypass -File .\configurar_seguridad.ps1

# 3. Aplicar las migraciones (crea las tablas Usuarios y Registros)
dotnet ef database update --project src/Bitacora.Infrastructure --startup-project src/Bitacora.Api

# 4. Ejecutar la API
dotnet run --project src/Bitacora.Api
```

La API queda en **https://localhost:7080** (Swagger en **https://localhost:7080/swagger**). Las peticiones a `http://localhost:5080` se redirigen a HTTPS.

Si prefieres configurar los secretos a mano:

```powershell
dotnet user-secrets set "Jwt:Clave" "<clave-aleatoria-de-al-menos-32-caracteres>" --project src/Bitacora.Api
dotnet user-secrets set "AdminInicial:Correo" "admin@bitacora.com" --project src/Bitacora.Api
dotnet user-secrets set "AdminInicial:Contrasena" "<contraseña-del-admin>" --project src/Bitacora.Api
```

### Probar en Swagger

1. `POST /api/auth/login` con el correo y la contraseña.
2. Copia el `token` de la respuesta y pulsa **Authorize** (arriba a la derecha).
3. Pega solo el token y prueba los endpoints de registros.

### Probar desde Android (app `BitacoraApp`)

El emulador no confía en el certificado de desarrollo, así que para las pruebas locales la API se ejecuta con un perfil solo HTTP:

```powershell
dotnet run --project src/Bitacora.Api --launch-profile android
```

La app usa `http://10.0.2.2:5080/` (el `localhost` del PC visto desde el emulador). En producción todo debe ir por HTTPS.

### Alternativa: Docker

Copia `docker.ejemplo.txt` como `.env`, pon tus valores (el archivo `.env` no se sube al repositorio) y ejecuta:

```bash
docker compose up --build
```

## Endpoints

| Método | Ruta | Acceso | Respuestas |
|---|---|---|---|
| POST | `/api/auth/registro` | Anónimo | 201, 400, 409 |
| POST | `/api/auth/login` | Anónimo | 200, 400, 401 |
| GET | `/api/registros` | Token | 200, 401 |
| GET | `/api/registros/{id}` | Token | 200, 401, 404 |
| POST | `/api/registros` | Token | 201 + `Location`, 400, 401 |
| PUT | `/api/registros/{id}` | Token (dueño o Admin) | 200, 400, 401, 403, 404 |
| DELETE | `/api/registros/{id}` | Token con rol **Admin** | 204, 401, 403, 404 |

## Pruebas con Postman

1. Importa `postman/Bitacora.postman_collection.json` y el entorno `postman/Bitacora.local.postman_environment.json` (lo crea `configurar_seguridad.ps1` con tus valores; no se sube al repositorio).
2. Con la API en ejecución, abre la colección → **Run**.

La carpeta **3. Seguridad - pruebas obligatorias** cubre:

| # | Prueba | Esperado |
|---|---|---|
| 1 | Petición sin token | 401 |
| 2 | Token de Usuario intentando DELETE | 403 |
| 3 | Token de Admin haciendo DELETE | 204 |
| 4 | Usuario modificando un registro ajeno | 403 |
| 5 | Token alterado / token vencido | 401 |
| 6 | Credenciales incorrectas en el login (correo o contraseña) | 401 con el mismo mensaje |

## Laboratorio: Volley vs OkHttp vs Retrofit

En la app Android, menú ⋮ → **Laboratorio** (`ComparativaActivity`). Cada botón consulta `GET /api/registros` con una librería distinta y muestra la cantidad de registros, el tiempo en milisegundos y el hilo en que llegó la respuesta. Los resultados también se escriben en Logcat con el tag `Comparativa`.

| Librería | Clase | Encabezado del token | JSON | Hilo de la respuesta |
|---|---|---|---|---|
| Volley | `laboratorio/ClienteVolley.java` | `getHeaders()` | `JsonArrayRequest` (JSONArray) | Principal |
| OkHttp | `laboratorio/ClienteOkHttp.java` | `Request.Builder().header(...)` | Gson manual | Hilo de fondo (se pasa al principal con `Handler`) |
| Retrofit | `laboratorio/ClienteRetrofit.java` | `@Header` en `BitacoraApi` | `GsonConverterFactory` | Principal |

Las tres usan el mismo tiempo de espera (10 s) y sin reintentos, para comparar en igualdad de condiciones.

Escenarios de falla: API detenida, modo avión, tiempo agotado (`GET /api/diagnostico/lento?segundos=15`, endpoint **temporal** solo en Development), sin token o token alterado (401) y error 500 (`GET /api/diagnostico/error`).

## Integrantes y participación

| Integrante | Rama | Responsabilidades |
|---|---|---|
| Ovidio Isai Castillo | `main` | Estructura de la solución, capas de dominio, aplicación, infraestructura y API; autenticación JWT, autorización por roles y por dueño; app Android. |
| Oliver Claudio Avila | `feature/pruebas-docs` | Colección y entorno de Postman, pruebas de cada endpoint y de seguridad, diagrama de capas e informe. |

## Flujo de trabajo en GitHub

- Rama principal `main`; cada integrante trabaja en su rama y abre un *pull request*.
- Mensajes de commit descriptivos, por ejemplo `feat(auth): login con JWT`.
- **Sin secretos en el historial**: `bin/`, `obj/`, `*.db`, `.env` y los entornos locales de Postman están en `.gitignore`; la clave JWT vive en `dotnet user-secrets`.
