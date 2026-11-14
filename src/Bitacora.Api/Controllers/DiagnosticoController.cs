using Bitacora.Application.DTOs;
using Bitacora.Application.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Bitacora.Api.Controllers;

/// <summary>
/// Endpoints de apoyo para pruebas (escenarios de falla). TEMPORALES: solo responden
/// en el entorno Development y deben retirarse antes de publicar la API.
/// </summary>
[ApiController]
[Route("api/diagnostico")]
public class DiagnosticoController : ControllerBase
{
    private readonly IWebHostEnvironment _entorno;
    private readonly IRegistroService _registros;

    public DiagnosticoController(IWebHostEnvironment entorno, IRegistroService registros)
    {
        _entorno = entorno;
        _registros = registros;
    }

    /// <summary>Lanza una excepción no controlada para comprobar la respuesta 500 con ProblemDetails.</summary>
    /// <response code="500">Error genérico sin detalles internos.</response>
    [HttpGet("error")]
    [AllowAnonymous]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status500InternalServerError)]
    public IActionResult ProvocarError()
    {
        if (!_entorno.IsDevelopment())
        {
            return NotFound();
        }

        throw new InvalidOperationException("Detalle interno que NO debe llegar al cliente: Data Source=bitacora.db");
    }

    /// <summary>
    /// Igual que GET /api/registros pero espera antes de responder, para provocar
    /// el tiempo de espera agotado en los clientes (configurados en 10 s).
    /// </summary>
    /// <param name="segundos">Segundos de espera (1 a 60). Por defecto 15.</param>
    /// <param name="ct">Token de cancelación (se cancela si el cliente se desconecta).</param>
    /// <response code="200">Listado de registros, después de la espera.</response>
    /// <response code="401">Falta el token o no es válido.</response>
    [HttpGet("lento")]
    [Authorize]
    [ProducesResponseType(typeof(IReadOnlyList<RegistroResumenDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> Lento([FromQuery] int segundos = 15, CancellationToken ct = default)
    {
        if (!_entorno.IsDevelopment())
        {
            return NotFound();
        }

        await Task.Delay(TimeSpan.FromSeconds(Math.Clamp(segundos, 1, 60)), ct);
        return Ok(await _registros.ListarAsync(ct));
    }
}
