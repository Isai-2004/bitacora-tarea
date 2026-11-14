namespace Bitacora.Domain.Rules;

/// <summary>Roles válidos de los usuarios.</summary>
public static class Roles
{
    public const string Admin = "Admin";
    public const string Usuario = "Usuario";

    public static bool EsValido(string? rol) => rol is Admin or Usuario;
}
