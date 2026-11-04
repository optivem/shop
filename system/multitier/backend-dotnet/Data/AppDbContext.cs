using Microsoft.EntityFrameworkCore;
using MyCompany.MyShop.Backend.Core.Entities;

namespace MyCompany.MyShop.Backend.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    /// <summary>The unique constraint on (owner, idempotency_key); a violation means a concurrent first use won.</summary>
    public const string IdempotencyKeyUniqueConstraint = "uq_idempotency_keys_owner_key";

    public DbSet<Order> Orders { get; set; } = null!;
    public DbSet<Coupon> Coupons { get; set; } = null!;
    public DbSet<IdempotencyKey> IdempotencyKeys { get; set; } = null!;

    public override Task<int> SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        BumpOrderVersions();
        return base.SaveChangesAsync(cancellationToken);
    }

    public override int SaveChanges()
    {
        BumpOrderVersions();
        return base.SaveChanges();
    }

    private void BumpOrderVersions()
    {
        foreach (var entry in ChangeTracker.Entries<Order>().Where(e => e.State == EntityState.Modified))
        {
            var version = entry.Property(e => e.Version);
            version.CurrentValue = version.OriginalValue + 1;
        }
    }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Order>(entity =>
        {
            entity.HasIndex(e => e.OrderNumber).IsUnique();
            entity.Property(e => e.Status).HasConversion<string>().HasMaxLength(50);
            entity.Property(e => e.Country).HasDefaultValueSql("'US'");
            entity.Property(e => e.BasePrice).HasDefaultValueSql("0");
            entity.Property(e => e.DiscountRate).HasDefaultValueSql("0");
            entity.Property(e => e.DiscountAmount).HasDefaultValueSql("0");
            entity.Property(e => e.SubtotalPrice).HasDefaultValueSql("0");
            entity.Property(e => e.TaxRate).HasDefaultValueSql("0");
            entity.Property(e => e.TaxAmount).HasDefaultValueSql("0");
        });

        modelBuilder.Entity<Coupon>(entity =>
        {
            entity.HasIndex(e => e.Code).IsUnique();
            entity.Property(e => e.UsedCount).HasDefaultValueSql("0");
        });

        modelBuilder.Entity<IdempotencyKey>(entity =>
        {
            entity.HasIndex(e => new { e.Owner, e.Key }).IsUnique().HasDatabaseName(IdempotencyKeyUniqueConstraint);
            entity.HasIndex(e => e.CreatedAt).HasDatabaseName("idx_idempotency_keys_created_at");
            entity.Property(e => e.ResponseBody).HasColumnType("text");
        });
    }
}
