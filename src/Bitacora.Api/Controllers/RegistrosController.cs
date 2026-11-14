using Bitacora.Api.Seguridad;
using Bitacora.Application.DTOs;
using Bitacora.Application.Services;
using Bitacora.Domain.Rules;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Bitacora.Api.Controllers;

/// <summary>Administración de los registros de la bitácora. Requiere un token válido.</summary>
[ApiController]
[Route("api/registros")]
[Authorize]
[ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status401Unauthorized)]
public class RegistrosController : ControllerBase
{
    private readonly IRegistroService _servicio;

    public RegistrosController(IRegistroService servicio)
    {
        _servicio = servicio;
    }

    /// <summary>Lista los registros (sin la foto completa).</summary>
    /// <response code="200">Listado de registros, del más reciente al más antiguo.</response>
    /// <response code="401">Falta el token o no es válido.</response>
    [HttpGet]
    [ProducesResponseType(typeof(IReadOnlyList<RegistroResumenDto>), StatusCodes.Status200OK)]
    public async Task<ActionResult<IReadOnlyList<RegistroResumenDto>>> Listar(CancellationToken ct)
    {
        return Ok(await _servicio.ListarAsync(ct));
    }

    /// <summary>Consulta un registro con su foto.</summary>
    /// <param name="id">Identificador del registro.</param>
    /// <param name="ct">Token de cancelación.</param>
    /// <response code="200">Registro encontrado.</response>
    /// <response code="404">No existe un registro con ese id.</response>
    [HttpGet("{id:int}")]
    [ProducesResponseType(typeof(RegistroDetalleDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<RegistroDetalleDto>> ObtenerPorId(int id, CancellationToken ct)
    {
        return Ok(await _servicio.ObtenerAsync(id, ct));
    }

    /// <summary>Crea un registro. La fecha y el dueño (usuario del token) los asigna el servidor.</summary>
    /// <response code="201">Registro creado; el encabezado Location apunta al nuevo recurso.</response>
    /// <response code="400">Datos inválidos; se detallan los campos con error.</response>
    [HttpPost]
    [Consumes("application/json")]
    [ProducesResponseType(typeof(RegistroDetalleDto), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    public async Task<ActionResult<RegistroDetalleDto>> Crear([FromBody] CrearRegistroDto dto, CancellationToken ct)
    {
        var creado = await _servicio.CrearAsync(dto, User.ObtenerUsuarioActual(), ct);
        return CreatedAtAction(nameof(ObtenerPorId), new { id = creado.Id }, creado);
    }

    /// <summary>Actualiza un registro. Un Usuario solo puede modificar los suyos; el Admin, cualquiera.</summary>
    /// <param name="id">Identificador del registro.</param>
    /// <param name="dto">Nuevos datos.</param>
    /// <param name="ct">Token de cancelación.</param>
    /// <response code="200">Registro actualizado.</response>
    /// <response code="400">Datos inválidos.</response>
    /// <response code="403">El registro pertenece a otro usuario.</response>
    /// <response code="404">No existe un registro con ese id.</response>
    [HttpPut("{id:int}")]
    [Consumes("application/json")]
    [ProducesResponseType(typeof(RegistroDetalleDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ValidationProblemDetails), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<ActionResult<RegistroDetalleDto>> Actualizar(int id, [FromBody] ActualizarRegistroDto dto, CancellationToken ct)
    {
        return Ok(await _servicio.ActualizarAsync(id, dto, User.ObtenerUsuarioActual(), ct));
    }

    /// <summary>Elimina un registro. Solo el rol Admin.</summary>
    /// <param name="id">Identificador del registro.</param>
    /// <param name="ct">Token de cancelación.</param>
    /// <response code="204">Registro eliminado.</response>
    /// <response code="403">El usuario no es Admin.</response>
    /// <response code="404">No existe un registro con ese id.</response>
    [HttpDelete("{id:int}")]
    [Authorize(Roles = Roles.Admin)]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Eliminar(int id, CancellationToken ct)
    {
        await _servicio.EliminarAsync(id, User.ObtenerUsuarioActual(), ct);
        return NoContent();
    }
}
