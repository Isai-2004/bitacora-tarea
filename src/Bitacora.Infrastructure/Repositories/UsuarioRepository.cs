using Bitacora.Application.Interfaces;
using Bitacora.Domain.Entities;
using Bitacora.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Bitacora.Infrastructure.Repositories;

public class UsuarioRepository : IUsuarioRepository
{
    private readonly BitacoraDbContext _db;

    public UsuarioRepository(BitacoraDbContext db)
    {
        _db = db;
    }

    public Task<Usuario?> ObtenerPorCorreoAsync(string correoNormalizado, CancellationToken ct = default) =>
        _db.Usuarios.FirstOrDefaultAsync(u => u.Correo == correoNormalizado, ct);

    public Task<bool> ExisteCorreoAsync(string correoNormalizado, CancellationToken ct = default) =>
        _db.Usuarios.AnyAsync(u => u.Correo == correoNormalizado, ct);

    public async Task AgregarAsync(Usuario usuario, CancellationToken ct = default)
    {
        _db.Usuarios.Add(usuario);
        await _db.SaveChangesAsync(ct);
    }

    public Task GuardarCambiosAsync(CancellationToken ct = default) => _db.SaveChangesAsync(ct);
}
