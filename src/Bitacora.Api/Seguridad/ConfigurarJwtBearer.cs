using System.Diagnostics;
using Bitacora.Infrastructure.Seguridad;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;

namespace Bitacora.Api.Seguridad;

/// <summary>
/// Configura la validación del JWT: emisor, audiencia, firma y tiempo de vida.
/// Se aplica de forma diferida para leer la clave desde user-secrets / variables de entorno.
/// </summary>
public class ConfigurarJwtBearer : IConfigureNamedOptions<JwtBearerOptions>
{
    private readonly JwtOpciones _jwt;

    public ConfigurarJwtBearer(IOptions<JwtOpciones> jwt)
    {
        _jwt = jwt.Value;
    }

    public void Configure(string? name, JwtBearerOptions options)
    {
        if (name != JwtBearerDefaults.AuthenticationScheme)
        {
            return;
        }

        // Usa los nombres de claim tal como vienen en el token ("sub", "role").
        options.MapInboundClaims = false;

        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = _jwt.Emisor,
            ValidateAudience = true,
            ValidAudience = _jwt.Audiencia,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = GeneradorTokenJwt.CrearClave(_jwt.Clave),
            ValidAlgorithms = new[] { SecurityAlgorithms.HmacSha256 },
            ValidateLifetime = true,
            RequireExpirationTime = true,
            // Sin margen de tolerancia: el token vence exactamente a los N minutos.
            ClockSkew = TimeSpan.Zero,
            NameClaimType = "sub",
            RoleClaimType = "role"
        };

        options.Events = new JwtBearerEvents
        {
            // 401: no autenticado (sin token, token alterado o vencido).
            OnChallenge = async contexto =>
            {
                contexto.HandleResponse();

                var detalle = contexto.AuthenticateFailure switch
                {
                    SecurityTokenExpiredException => "El token venció. Inicia sesión de nuevo.",
                    not null => "El token no es válido.",
                    _ => "Debes iniciar sesión para acceder a este recurso."
                };

                contexto.Response.StatusCode = StatusCodes.Status401Unauthorized;
                contexto.Response.Headers.WWWAuthenticate = contexto.AuthenticateFailure is null
                    ? "Bearer"
                    : "Bearer error=\"invalid_token\"";

                await EscribirProblemaAsync(contexto.HttpContext, StatusCodes.Status401Unauthorized,
                    "No autenticado.", detalle, "https://tools.ietf.org/html/rfc9110#section-15.5.2");
            },

            // 403: autenticado, pero sin el rol necesario.
            OnForbidden = contexto => EscribirProblemaAsync(contexto.HttpContext, StatusCodes.Status403Forbidden,
                "Acceso prohibido.", "No tienes permiso para realizar esta acción.",
                "https://tools.ietf.org/html/rfc9110#section-15.5.4")
        };
    }

    public void Configure(JwtBearerOptions options) => Configure(Options.DefaultName, options);

    private static Task EscribirProblemaAsync(HttpContext http, int status, string titulo, string detalle, string tipo)
    {
        var problema = new ProblemDetails
        {
            Status = status,
            Title = titulo,
            Detail = detalle,
            Type = tipo,
            Instance = http.Request.Path
        };
        problema.Extensions["traceId"] = Activity.Current?.Id ?? http.TraceIdentifier;

        http.Response.StatusCode = status;
        return http.Response.WriteAsJsonAsync(problema, typeof(ProblemDetails), options: null,
            contentType: "application/problem+json");
    }
}
