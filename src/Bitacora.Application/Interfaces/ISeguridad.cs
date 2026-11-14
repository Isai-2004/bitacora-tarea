using Bitacora.Application.DTOs;
using Bitacora.Domain.Entities;

namespace Bitacora.Application.Interfaces;

/// <summary>Persistencia de usuarios.</summary>
public interface IUsuarioRepository
{
    Task<Usuario?> ObtenerPorCorreoAsync(string correoNormalizado, CancellationToken ct = default);
    Task<bool> ExisteCorreoAsync(string correoNormalizado, CancellationToken ct = default);
    Task AgregarAsync(Usuario usuario, CancellationToken ct = default);
    Task GuardarCambiosAsync(CancellationToken ct = default);
}

/// <summary>Calcula y verifica hashes de contraseñas (nunca se guardan en texto plano).</summary>
public interface IHasherContrasenas
{
    string Hashear(string contrasena);

    /// <summary>Verifica la contraseña. Si el hash usa parámetros antiguos, indica que conviene recalcularlo.</summary>
    ResultadoVerificacion Verificar(string hash, string contrasena);
}

public enum ResultadoVerificacion
{
    Fallida,
    Correcta,
    CorrectaRequiereRehash
}

/// <summary>Emite los tokens JWT.</summary>
public interface IGeneradorToken
{
    TokenGenerado Generar(Usuario usuario);
}
