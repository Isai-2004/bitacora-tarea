using Bitacora.Application.DTOs;
using Bitacora.Application.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Bitacora.Api.Controllers;

/// <summary>Registro de cuentas e inicio de sesión (no requieren token).</summary>
[ApiController]
[Route("api/auth")]
[AllowAnonymous]
public class AuthController : ControllerBase
{
    private readonly IAuthService _auth;

    public AuthController(IAuthService auth)
    {
        _auth = auth;
    }

    /// <summary>Crea una cuenta con el rol Usuario. La contraseña se guarda como hash.</summary>
    /// <response code="201">Cuenta creada.</response>
    /// <response code="400">Datos inválidos (correo o contraseña).</response>
    /// <response code="409">Ya existe una cuenta con ese correo.</response>
    [HttpPost("registro")]
    [Consumes("application/json")]
    [ProducesResponseType(typeof(UsuarioDto), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status409Conflict)]
    public async Task<ActionResult<UsuarioDto>> Registro([FromBody] RegistroUsuarioDto dto, CancellationToken ct)
    {
        var usuario = await _auth.RegistrarAsync(dto, ct);
        return StatusCode(StatusCodes.Status201Created, usuario);
    }

    /// <summary>Inicia sesión y devuelve el token JWT, su vencimiento y el rol.</summary>
    /// <response code="200">Credenciales correctas.</response>
    /// <response code="400">Faltan datos.</response>
    /// <response code="401">Correo o contraseña incorrectos (no se indica cuál de los dos).</response>
    [HttpPost("login")]
    [Consumes("application/json")]
    [ProducesResponseType(typeof(LoginRespuestaDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    public async Task<ActionResult<LoginRespuestaDto>> Login([FromBody] LoginDto dto, CancellationToken ct)
    {
        return Ok(await _auth.LoginAsync(dto, ct));
    }
}
