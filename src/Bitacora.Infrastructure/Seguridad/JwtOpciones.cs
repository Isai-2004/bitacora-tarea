namespace Bitacora.Infrastructure.Seguridad;

/// <summary>
/// Configuración del JWT (sección "Jwt"). La Clave NO está en appsettings.json:
/// se define con dotnet user-secrets o con la variable de entorno Jwt__Clave.
/// </summary>
public class JwtOpciones
{
    public const string Seccion = "Jwt";

    /// <summary>Quién emite el token (claim iss).</summary>
    public string Emisor { get; set; } = string.Empty;

    /// <summary>Para quién es el token (claim aud).</summary>
    public string Audiencia { get; set; } = string.Empty;

    /// <summary>Clave secreta de firma HMAC-SHA256 (mínimo 32 caracteres).</summary>
    public string Clave { get; set; } = string.Empty;

    /// <summary>Minutos de vida del token. Para las pruebas: 10.</summary>
    public int MinutosExpiracion { get; set; } = 10;

    public const int LongitudMinimaClave = 32;
}
