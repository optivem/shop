using Microsoft.EntityFrameworkCore;
using Optivem.Starter.Monolith.Core.Entities;

namespace Optivem.Starter.Monolith.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    public DbSet<Order> Orders { get; set; } = null!;

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Order>(entity =>
        {
            entity.HasIndex(e => e.OrderNumber).IsUnique();
            entity.Property(e => e.Status).HasConversion<string>();
        });
    }
}
