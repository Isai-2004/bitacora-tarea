using System.Reflection;
using Bitacora.Api.Middleware;
using Bitacora.Api.Seguridad;
using Bitacora.Application;
using Bitacora.Infrastructure;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.OpenApi.Models;

var builder = WebApplication.CreateBuilder(args);

// Capas de la solución (inyección de dependencias).
builder.Services.AddApplication();
builder.Services.AddInfrastructure(builder.Configuration);

// Autenticación con JWT Bearer: valida emisor, audiencia, firma y tiempo de vida.
// La configuración se lee de forma diferida (ConfigurarJwtBearer) porque la clave
// viene de user-secrets o de variables de entorno.
builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer();
builder.Services.ConfigureOptions<ConfigurarJwtBearer>();
builder.Services.AddAuthorization();

// Controladores: [ApiController] responde 400 automáticamente con los campos inválidos.
builder.Services.AddControllers();

// Manejo centralizado de errores con ProblemDetails.
builder.Services.AddProblemDetails();
builder.Services.AddExceptionHandler<ManejadorGlobalExcepciones>();

// OpenAPI + Swagger UI con el esquema Bearer para probar con token.
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "Bitácora API",
        Version = "v1",
        Description = "API REST para administrar los registros de la bitácora (arquitectura limpia). " +
                      "Inicia sesión en POST /api/auth/login, copia el token y pulsa Authorize."
    });

    var esquemaBearer = new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Pega solo el token (sin la palabra Bearer).",
        Reference = new OpenApiReference { Type = ReferenceType.SecurityScheme, Id = "Bearer" }
    };
    c.AddSecurityDefinition("Bearer", esquemaBearer);
    c.AddSecurityRequirement(new OpenApiSecurityRequirement { [esquemaBearer] = Array.Empty<string>() });

    // Comentarios XML de la API y de los DTO (capa de aplicación).
    foreach (var ensamblado in new[] { Assembly.GetExecutingAssembly(), typeof(Bitacora.Application.DependencyInjection).Assembly })
    {
        var xml = Path.Combine(AppContext.BaseDirectory, $"{ensamblado.GetName().Name}.xml");
        if (File.Exists(xml))
        {
            c.IncludeXmlComments(xml);
        }
    }
});

var app = builder.Build();

// Con Docker se activa esta opción para crear/actualizar la base de datos al arrancar.
if (app.Configuration.GetValue<bool>("BaseDeDatos:AplicarMigracionesAlIniciar"))
{
    app.Services.AplicarMigraciones();
}

// Crea el administrador inicial (credenciales en user-secrets o variables de entorno).
await app.Services.CrearAdminInicialAsync(app.Configuration);

app.UseExceptionHandler();
app.UseStatusCodePages();

app.UseHttpsRedirection();

app.UseSwagger();
app.UseSwaggerUI(c =>
{
    c.SwaggerEndpoint("/swagger/v1/swagger.json", "Bitácora API v1");
    c.DocumentTitle = "Bitácora API";
});

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();

// Permite referenciar Program desde pruebas de integración.
public partial class Program { }
