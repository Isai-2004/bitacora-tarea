using System.Security.Claims;
using Bitacora.Application.DTOs;

namespace Bitacora.Api.Seguridad;

public static class ClaimsPrincipalExtensions
{
    /// <summary>Obtiene el id y el rol del usuario a partir de los claims del token.</summary>
    public static UsuarioActual ObtenerUsuarioActual(this ClaimsPrincipal usuario)
    {
        var sub = usuario.FindFirst("sub")?.Value;
        var rol = usuario.FindFirst("role")?.Value ?? string.Empty;

        if (!int.TryParse(sub, out var id))
        {
            // No debería ocurrir con un token emitido por esta API.
            throw new UnauthorizedAccessException("El token no contiene el identificador del usuario.");
        }

        return new UsuarioActual(id, rol);
    }
}
