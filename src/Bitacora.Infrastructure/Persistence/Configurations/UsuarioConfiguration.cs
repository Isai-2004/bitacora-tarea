using Bitacora.Domain.Entities;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace Bitacora.Infrastructure.Persistence.Configurations;

public class UsuarioConfiguration : IEntityTypeConfiguration<Usuario>
{
    public void Configure(EntityTypeBuilder<Usuario> builder)
    {
        builder.ToTable("Usuarios");
        builder.HasKey(u => u.Id);

        builder.Property(u => u.Correo)
            .IsRequired()
            .HasMaxLength(Usuario.CorreoLongitudMaxima);
        builder.HasIndex(u => u.Correo).IsUnique();

        // Solo se guarda el hash (incluye sal y parámetros), nunca la contraseña.
        builder.Property(u => u.ContrasenaHash).IsRequired();

        builder.Property(u => u.Rol)
            .IsRequired()
            .HasMaxLength(20);

        builder.Property(u => u.FechaRegistro)
            .IsRequired()
            .HasConversion(
                v => v.ToUniversalTime(),
                v => DateTime.SpecifyKind(v, DateTimeKind.Utc));

        builder.Ignore(u => u.EsAdmin);
    }
}
