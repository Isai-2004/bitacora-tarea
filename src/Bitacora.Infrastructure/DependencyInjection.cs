using Bitacora.Application.Interfaces;
using Bitacora.Infrastructure.Persistence;
using Bitacora.Infrastructure.Repositories;
using Bitacora.Infrastructure.Seguridad;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace Bitacora.Infrastructure;

public static class DependencyInjection
{
    /// <summary>Registra el DbContext (SQLite), los repositorios y los servicios de seguridad.</summary>
    public static IServiceCollection AddInfrastructure(this IServiceCollection services, IConfiguration configuration)
    {
        var cadena = configuration.GetConnectionString("DefaultConnection")
                     ?? "Data Source=bitacora.db";

        services.AddDbContext<BitacoraDbContext>(options => options.UseSqlite(cadena));
        services.AddScoped<IRegistroRepository, RegistroRepository>();
        services.AddScoped<IUsuarioRepository, UsuarioRepository>();

        // La clave se valida al arrancar la API: si falta o es corta, la API no inicia.
        services.AddOptions<JwtOpciones>()
            .Bind(configuration.GetSection(JwtOpciones.Seccion))
            .Validate(o => !string.IsNullOrWhiteSpace(o.Emisor) && !string.IsNullOrWhiteSpace(o.Audiencia),
                "Faltan Jwt:Emisor o Jwt:Audiencia en appsettings.json.")
            .Validate(o => o.Clave.Length >= JwtOpciones.LongitudMinimaClave,
                $"Falta la clave de firma Jwt:Clave (mínimo {JwtOpciones.LongitudMinimaClave} caracteres). " +
                "Defínela con: dotnet user-secrets set \"Jwt:Clave\" \"<clave>\" --project src/Bitacora.Api")
            .Validate(o => o.MinutosExpiracion > 0, "Jwt:MinutosExpiracion debe ser mayor que cero.")
            .ValidateOnStart();

        services.AddSingleton<IHasherContrasenas, HasherContrasenas>();
        services.AddSingleton<IGeneradorToken, GeneradorTokenJwt>();

        return services;
    }
}
