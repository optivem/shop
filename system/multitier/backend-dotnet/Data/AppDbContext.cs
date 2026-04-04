using Microsoft.EntityFrameworkCore;
using Optivem.Shop.Backend.Core.Entities;

namespace Optivem.Shop.Backend.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    public DbSet<Order> Orders { get; set; } = null!;
    public DbSet<Coupon> Coupons { get; set; } = null!;

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Order>(entity =>
        {
            entity.HasIndex(e => e.OrderNumber).IsUnique();
            entity.Property(e => e.Status).HasConversion<string>();
        });

        modelBuilder.Entity<Coupon>(entity =>
        {
            entity.HasIndex(e => e.Code).IsUnique();
        });
    }
}
