using Bitacora.Application.Interfaces;
using Bitacora.Domain.Entities;
using Microsoft.AspNetCore.Identity;

namespace Bitacora.Infrastructure.Seguridad;

/// <summary>
/// Usa PasswordHasher de ASP.NET Core Identity: PBKDF2 con HMAC-SHA512, sal aleatoria
/// de 128 bits y 100 000 iteraciones. El hash incluye la sal y los parámetros.
/// </summary>
public class HasherContrasenas : IHasherContrasenas
{
    private readonly PasswordHasher<Usuario> _hasher = new();

    public string Hashear(string contrasena) => _hasher.HashPassword(null!, contrasena);

    public ResultadoVerificacion Verificar(string hash, string contrasena) =>
        _hasher.VerifyHashedPassword(null!, hash, contrasena) switch
        {
            PasswordVerificationResult.Success => ResultadoVerificacion.Correcta,
            PasswordVerificationResult.SuccessRehashNeeded => ResultadoVerificacion.CorrectaRequiereRehash,
            _ => ResultadoVerificacion.Fallida
        };
}
