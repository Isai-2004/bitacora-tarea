using Bitacora.Domain.Exceptions;
using Bitacora.Domain.Rules;

namespace Bitacora.Domain.Entities;

/// <summary>
/// Entrada de la bitácora. La entidad protege sus propias reglas:
/// solo puede crearse o modificarse con datos válidos.
/// </summary>
public class Registro
{
    public int Id { get; private set; }
    public string Titulo { get; private set; } = string.Empty;
    public string Descripcion { get; private set; } = string.Empty;
    public string? FotoBase64 { get; private set; }

    /// <summary>Fecha en UTC asignada por el servidor al crear el registro.</summary>
    public DateTime FechaCreacion { get; private set; }

    /// <summary>Usuario que creó el registro (su dueño).</summary>
    public int UsuarioId { get; private set; }
    public Usuario? Usuario { get; private set; }

    // Constructor requerido por Entity Framework Core.
    private Registro() { }

    /// <summary>Crea un registro nuevo. La fecha y el dueño los asigna el servidor.</summary>
    public static Registro Crear(string titulo, string descripcion, string? fotoBase64, int usuarioId)
    {
        if (usuarioId <= 0)
        {
            throw new ReglaNegocioException(nameof(UsuarioId), "El registro debe pertenecer a un usuario.");
        }

        var registro = new Registro
        {
            FechaCreacion = DateTime.UtcNow,
            UsuarioId = usuarioId
        };
        registro.AsignarDatos(titulo, descripcion, fotoBase64);
        return registro;
    }

    /// <summary>Actualiza el contenido. La fecha de creación y el dueño no cambian.</summary>
    public void Actualizar(string titulo, string descripcion, string? fotoBase64)
    {
        AsignarDatos(titulo, descripcion, fotoBase64);
    }

    /// <summary>Indica si el registro pertenece al usuario indicado.</summary>
    public bool PerteneceA(int usuarioId) => UsuarioId == usuarioId;

    private void AsignarDatos(string titulo, string descripcion, string? fotoBase64)
    {
        titulo = titulo?.Trim() ?? string.Empty;
        descripcion = descripcion?.Trim() ?? string.Empty;
        fotoBase64 = string.IsNullOrWhiteSpace(fotoBase64) ? null : fotoBase64.Trim();

        if (titulo.Length < ReglasRegistro.TituloLongitudMinima ||
            titulo.Length > ReglasRegistro.TituloLongitudMaxima)
        {
            throw new ReglaNegocioException(nameof(Titulo),
                $"El título debe tener entre {ReglasRegistro.TituloLongitudMinima} y {ReglasRegistro.TituloLongitudMaxima} caracteres.");
        }

        if (descripcion.Length == 0)
        {
            throw new ReglaNegocioException(nameof(Descripcion), "La descripción es obligatoria.");
        }

        if (descripcion.Length > ReglasRegistro.DescripcionLongitudMaxima)
        {
            throw new ReglaNegocioException(nameof(Descripcion),
                $"La descripción no puede superar {ReglasRegistro.DescripcionLongitudMaxima} caracteres.");
        }

        if (fotoBase64 is not null)
        {
            if (!ReglasRegistro.EsBase64Valido(fotoBase64, out var bytes))
            {
                throw new ReglaNegocioException(nameof(FotoBase64), "La foto no es un texto Base64 válido.");
            }

            if (bytes > ReglasRegistro.FotoTamanoMaximoBytes)
            {
                throw new ReglaNegocioException(nameof(FotoBase64), "La foto no puede superar 2 MB.");
            }
        }

        Titulo = titulo;
        Descripcion = descripcion;
        FotoBase64 = fotoBase64;
    }
}
