using Bitacora.Application.DTOs;
using Bitacora.Application.Exceptions;
using Bitacora.Application.Interfaces;
using Bitacora.Domain.Entities;
using Bitacora.Domain.Rules;

namespace Bitacora.Application.Services;

/// <summary>Casos de uso de registro e inicio de sesión.</summary>
public interface IAuthService
{
    Task<UsuarioDto> RegistrarAsync(RegistroUsuarioDto dto, CancellationToken ct = default);
    Task<LoginRespuestaDto> LoginAsync(LoginDto dto, CancellationToken ct = default);
    Task<UsuarioDto> CrearAdminSiNoExisteAsync(string correo, string contrasena, CancellationToken ct = default);
}

public class AuthService : IAuthService
{
    private readonly IUsuarioRepository _usuarios;
    private readonly IHasherContrasenas _hasher;
    private readonly IGeneradorToken _tokens;

    // Hash de relleno: se verifica cuando el correo no existe para que la respuesta
    // tarde lo mismo y no se pueda adivinar qué correos están registrados.
    private readonly Lazy<string> _hashDeRelleno;

    public AuthService(IUsuarioRepository usuarios, IHasherContrasenas hasher, IGeneradorToken tokens)
    {
        _usuarios = usuarios;
        _hasher = hasher;
        _tokens = tokens;
        _hashDeRelleno = new Lazy<string>(() => _hasher.Hashear(Guid.NewGuid().ToString()));
    }

    public async Task<UsuarioDto> RegistrarAsync(RegistroUsuarioDto dto, CancellationToken ct = default)
    {
        var correo = Usuario.NormalizarCorreo(dto.Correo);

        if (await _usuarios.ExisteCorreoAsync(correo, ct))
        {
            throw new ConflictoException(nameof(dto.Correo), "Ya existe una cuenta con ese correo.");
        }

        // El registro público siempre crea cuentas con el rol Usuario.
        var usuario = Usuario.Crear(correo, _hasher.Hashear(dto.Contrasena), Roles.Usuario);
        await _usuarios.AgregarAsync(usuario, ct);

        return new UsuarioDto(usuario.Id, usuario.Correo, usuario.Rol);
    }

    public async Task<LoginRespuestaDto> LoginAsync(LoginDto dto, CancellationToken ct = default)
    {
        var usuario = await _usuarios.ObtenerPorCorreoAsync(Usuario.NormalizarCorreo(dto.Correo), ct);

        if (usuario is null)
        {
            _hasher.Verificar(_hashDeRelleno.Value, dto.Contrasena);
            throw new CredencialesInvalidasException();
        }

        var resultado = _hasher.Verificar(usuario.ContrasenaHash, dto.Contrasena);
        if (resultado == ResultadoVerificacion.Fallida)
        {
            throw new CredencialesInvalidasException();
        }

        if (resultado == ResultadoVerificacion.CorrectaRequiereRehash)
        {
            usuario.ActualizarHash(_hasher.Hashear(dto.Contrasena));
            await _usuarios.GuardarCambiosAsync(ct);
        }

        var token = _tokens.Generar(usuario);
        return new LoginRespuestaDto(token.Token, "Bearer", token.Expira, usuario.Rol, usuario.Id, usuario.Correo);
    }

    public async Task<UsuarioDto> CrearAdminSiNoExisteAsync(string correo, string contrasena, CancellationToken ct = default)
    {
        correo = Usuario.NormalizarCorreo(correo);
        var existente = await _usuarios.ObtenerPorCorreoAsync(correo, ct);
        if (existente is not null)
        {
            return new UsuarioDto(existente.Id, existente.Correo, existente.Rol);
        }

        var admin = Usuario.Crear(correo, _hasher.Hashear(contrasena), Roles.Admin);
        await _usuarios.AgregarAsync(admin, ct);
        return new UsuarioDto(admin.Id, admin.Correo, admin.Rol);
    }
}
