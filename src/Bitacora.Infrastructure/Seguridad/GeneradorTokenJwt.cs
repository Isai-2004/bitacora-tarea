using System.Security.Claims;
using System.Text;
using Bitacora.Application.DTOs;
using Bitacora.Application.Interfaces;
using Bitacora.Domain.Entities;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;

namespace Bitacora.Infrastructure.Seguridad;

/// <summary>Emite JWT firmados con HMAC-SHA256.</summary>
public class GeneradorTokenJwt : IGeneradorToken
{
    private readonly JwtOpciones _opciones;
    private readonly JsonWebTokenHandler _handler = new();

    public GeneradorTokenJwt(IOptions<JwtOpciones> opciones)
    {
        _opciones = opciones.Value;
    }

    public TokenGenerado Generar(Usuario usuario)
    {
        var ahora = DateTime.UtcNow;
        var expira = ahora.AddMinutes(_opciones.MinutosExpiracion);

        var descriptor = new SecurityTokenDescriptor
        {
            Issuer = _opciones.Emisor,
            Audience = _opciones.Audiencia,
            IssuedAt = ahora,
            NotBefore = ahora,
            Expires = expira,
            Subject = new ClaimsIdentity(new[]
            {
                new Claim(JwtRegisteredClaimNames.Sub, usuario.Id.ToString()),
                new Claim(JwtRegisteredClaimNames.Email, usuario.Correo),
                new Claim("role", usuario.Rol),
                new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
            }),
            SigningCredentials = new SigningCredentials(
                CrearClave(_opciones.Clave), SecurityAlgorithms.HmacSha256)
        };

        return new TokenGenerado(_handler.CreateToken(descriptor), expira);
    }

    public static SymmetricSecurityKey CrearClave(string clave) => new(Encoding.UTF8.GetBytes(clave));
}
