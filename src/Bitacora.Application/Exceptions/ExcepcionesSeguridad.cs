namespace Bitacora.Application.Exceptions;

/// <summary>
/// Correo o contraseña incorrectos (HTTP 401). El mensaje es el mismo en ambos casos
/// para no revelar si el correo existe.
/// </summary>
public class CredencialesInvalidasException : Exception
{
    public const string MensajeGenerico = "Correo o contraseña incorrectos.";

    public CredencialesInvalidasException() : base(MensajeGenerico) { }
}

/// <summary>El usuario está autenticado pero no tiene permiso para la acción (HTTP 403).</summary>
public class AccesoProhibidoException : Exception
{
    public AccesoProhibidoException(string mensaje) : base(mensaje) { }
}

/// <summary>El recurso ya existe, por ejemplo un correo repetido (HTTP 409).</summary>
public class ConflictoException : Exception
{
    public string Campo { get; }

    public ConflictoException(string campo, string mensaje) : base(mensaje)
    {
        Campo = campo;
    }
}
