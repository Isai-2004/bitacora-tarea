using System.ComponentModel.DataAnnotations;
using Bitacora.Domain.Entities;

namespace Bitacora.Application.DTOs;

/// <summary>Datos para crear una cuenta. Siempre se crea con el rol Usuario.</summary>
public class RegistroUsuarioDto
{
    /// <example>ana@correo.com</example>
    [Required(ErrorMessage = "El correo es obligatorio.")]
    [EmailAddress(ErrorMessage = "El correo no es válido.")]
    [MaxLength(Usuario.CorreoLongitudMaxima, ErrorMessage = "El correo no puede superar {1} caracteres.")]
    public string Correo { get; set; } = string.Empty;

    /// <summary>Mínimo 8 caracteres, con al menos una letra y un número.</summary>
    /// <example>Clave#2026</example>
    [Required(ErrorMessage = "La contraseña es obligatoria.")]
    [StringLength(100, MinimumLength = 8, ErrorMessage = "La contraseña debe tener entre {2} y {1} caracteres.")]
    [RegularExpression(@"^(?=.*[A-Za-z])(?=.*\d).+$", ErrorMessage = "La contraseña debe tener al menos una letra y un número.")]
    public string Contrasena { get; set; } = string.Empty;
}

/// <summary>Credenciales para iniciar sesión.</summary>
public class LoginDto
{
    /// <example>ana@correo.com</example>
    [Required(ErrorMessage = "El correo es obligatorio.")]
    public string Correo { get; set; } = string.Empty;

    /// <example>Clave#2026</example>
    [Required(ErrorMessage = "La contraseña es obligatoria.")]
    public string Contrasena { get; set; } = string.Empty;
}

/// <summary>Datos públicos de un usuario (nunca incluye la contraseña ni su hash).</summary>
public record UsuarioDto(int Id, string Correo, string Rol);

/// <summary>Respuesta del inicio de sesión.</summary>
/// <param name="Token">JWT firmado que se envía en el encabezado Authorization: Bearer.</param>
/// <param name="TipoToken">Siempre "Bearer".</param>
/// <param name="Expira">Fecha y hora (UTC) en que el token deja de ser válido.</param>
/// <param name="Rol">Rol del usuario: Admin o Usuario.</param>
public record LoginRespuestaDto(
    string Token,
    string TipoToken,
    DateTime Expira,
    string Rol,
    int UsuarioId,
    string Correo);

/// <summary>Token generado junto con su vencimiento.</summary>
public record TokenGenerado(string Token, DateTime Expira);

/// <summary>Identidad del usuario autenticado que hace la petición (sale del token).</summary>
public record UsuarioActual(int Id, string Rol)
{
    public bool EsAdmin => Rol == Bitacora.Domain.Rules.Roles.Admin;
}
