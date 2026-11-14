using System.Net.Mail;
using Bitacora.Domain.Exceptions;
using Bitacora.Domain.Rules;

namespace Bitacora.Domain.Entities;

/// <summary>
/// Usuario de la API. La contraseña nunca se guarda en texto plano:
/// solo se almacena su hash (calculado en la capa de infraestructura).
/// </summary>
public class Usuario
{
    public const int CorreoLongitudMaxima = 256;

    public int Id { get; private set; }
    public string Correo { get; private set; } = string.Empty;
    public string ContrasenaHash { get; private set; } = string.Empty;
    public string Rol { get; private set; } = Roles.Usuario;
    public DateTime FechaRegistro { get; private set; }

    // Constructor requerido por Entity Framework Core.
    private Usuario() { }

    public static Usuario Crear(string correo, string contrasenaHash, string rol)
    {
        correo = NormalizarCorreo(correo);

        if (correo.Length == 0 || correo.Length > CorreoLongitudMaxima || !MailAddress.TryCreate(correo, out _))
        {
            throw new ReglaNegocioException(nameof(Correo), "El correo no es válido.");
        }

        if (string.IsNullOrWhiteSpace(contrasenaHash))
        {
            throw new ReglaNegocioException("Contrasena", "La contraseña es obligatoria.");
        }

        if (!Roles.EsValido(rol))
        {
            throw new ReglaNegocioException(nameof(Rol), "El rol debe ser Admin o Usuario.");
        }

        return new Usuario
        {
            Correo = correo,
            ContrasenaHash = contrasenaHash,
            Rol = rol,
            FechaRegistro = DateTime.UtcNow
        };
    }

    /// <summary>Reemplaza el hash (por ejemplo, cuando el algoritmo pide recalcularlo).</summary>
    public void ActualizarHash(string nuevoHash) => ContrasenaHash = nuevoHash;

    public bool EsAdmin => Rol == Roles.Admin;

    /// <summary>Los correos se comparan sin distinguir mayúsculas.</summary>
    public static string NormalizarCorreo(string? correo) => (correo ?? string.Empty).Trim().ToLowerInvariant();
}
