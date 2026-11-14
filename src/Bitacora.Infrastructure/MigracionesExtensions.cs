using Bitacora.Application.Services;
using Bitacora.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

namespace Bitacora.Infrastructure;

public static class MigracionesExtensions
{
    /// <summary>
    /// Aplica las migraciones pendientes al iniciar (equivale a "dotnet ef database update").
    /// Se usa al ejecutar con Docker, donde no está la herramienta dotnet-ef.
    /// </summary>
    public static void AplicarMigraciones(this IServiceProvider servicios)
    {
        using var scope = servicios.CreateScope();
        scope.ServiceProvider.GetRequiredService<BitacoraDbContext>().Database.Migrate();
    }

    /// <summary>
    /// Crea el administrador inicial si se configuró AdminInicial:Correo y AdminInicial:Contrasena
    /// (con user-secrets o variables de entorno; nunca en el repositorio).
    /// </summary>
    public static async Task CrearAdminInicialAsync(this IServiceProvider servicios, IConfiguration configuracion)
    {
        var correo = configuracion["AdminInicial:Correo"];
        var contrasena = configuracion["AdminInicial:Contrasena"];

        using var scope = servicios.CreateScope();
        var logger = scope.ServiceProvider.GetRequiredService<ILoggerFactory>().CreateLogger("AdminInicial");

        if (string.IsNullOrWhiteSpace(correo) || string.IsNullOrWhiteSpace(contrasena))
        {
            logger.LogWarning("No se configuró AdminInicial:Correo / AdminInicial:Contrasena; no se creó ningún administrador.");
            return;
        }

        var db = scope.ServiceProvider.GetRequiredService<BitacoraDbContext>();
        if ((await db.Database.GetPendingMigrationsAsync()).Any())
        {
            logger.LogWarning("Hay migraciones pendientes. Ejecuta 'dotnet ef database update' y reinicia la API para crear el administrador.");
            return;
        }

        var auth = scope.ServiceProvider.GetRequiredService<IAuthService>();
        var admin = await auth.CrearAdminSiNoExisteAsync(correo, contrasena);
        logger.LogInformation("Administrador disponible: {Correo}", admin.Correo);
    }
}
