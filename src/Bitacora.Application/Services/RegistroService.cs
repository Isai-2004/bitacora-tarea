using Bitacora.Application.DTOs;
using Bitacora.Application.Exceptions;
using Bitacora.Application.Interfaces;
using Bitacora.Application.Mappings;
using Bitacora.Domain.Entities;

namespace Bitacora.Application.Services;

/// <summary>Casos de uso de la bitácora. Todos reciben al usuario autenticado.</summary>
public interface IRegistroService
{
    Task<IReadOnlyList<RegistroResumenDto>> ListarAsync(CancellationToken ct = default);
    Task<RegistroDetalleDto> ObtenerAsync(int id, CancellationToken ct = default);
    Task<RegistroDetalleDto> CrearAsync(CrearRegistroDto dto, UsuarioActual usuario, CancellationToken ct = default);
    Task<RegistroDetalleDto> ActualizarAsync(int id, ActualizarRegistroDto dto, UsuarioActual usuario, CancellationToken ct = default);
    Task EliminarAsync(int id, UsuarioActual usuario, CancellationToken ct = default);
}

public class RegistroService : IRegistroService
{
    private const string Recurso = "registro";
    private readonly IRegistroRepository _repositorio;

    public RegistroService(IRegistroRepository repositorio)
    {
        _repositorio = repositorio;
    }

    public Task<IReadOnlyList<RegistroResumenDto>> ListarAsync(CancellationToken ct = default) =>
        _repositorio.ListarResumenAsync(ct);

    public async Task<RegistroDetalleDto> ObtenerAsync(int id, CancellationToken ct = default)
    {
        var registro = await ObtenerExistenteAsync(id, ct);
        return registro.ADetalle();
    }

    public async Task<RegistroDetalleDto> CrearAsync(CrearRegistroDto dto, UsuarioActual usuario, CancellationToken ct = default)
    {
        // El dueño sale del token, nunca del cuerpo de la petición.
        var registro = Registro.Crear(dto.Titulo, dto.Descripcion, dto.FotoBase64, usuario.Id);
        await _repositorio.AgregarAsync(registro, ct);

        var creado = await ObtenerExistenteAsync(registro.Id, ct);
        return creado.ADetalle();
    }

    public async Task<RegistroDetalleDto> ActualizarAsync(int id, ActualizarRegistroDto dto, UsuarioActual usuario, CancellationToken ct = default)
    {
        var registro = await ObtenerExistenteAsync(id, ct);

        // Un usuario normal solo puede modificar sus propios registros; el Admin, cualquiera.
        if (!usuario.EsAdmin && !registro.PerteneceA(usuario.Id))
        {
            throw new AccesoProhibidoException("Solo puedes modificar los registros que tú creaste.");
        }

        registro.Actualizar(dto.Titulo, dto.Descripcion, dto.FotoBase64);
        await _repositorio.ActualizarAsync(registro, ct);
        return registro.ADetalle();
    }

    public async Task EliminarAsync(int id, UsuarioActual usuario, CancellationToken ct = default)
    {
        // El controlador ya exige el rol Admin; se valida otra vez por defensa en profundidad.
        if (!usuario.EsAdmin)
        {
            throw new AccesoProhibidoException("Solo un administrador puede eliminar registros.");
        }

        var registro = await ObtenerExistenteAsync(id, ct);
        await _repositorio.EliminarAsync(registro, ct);
    }

    private async Task<Registro> ObtenerExistenteAsync(int id, CancellationToken ct) =>
        await _repositorio.ObtenerPorIdAsync(id, ct)
        ?? throw new RecursoNoEncontradoException(Recurso, id);
}
