using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Optivem.Shop.Monolith.Core.Entities;

[Table("coupons")]
public class Coupon
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    [Column("id")]
    public long Id { get; set; }

    [Required]
    [Column("code")]
    public string Code { get; set; } = null!;

    [Required]
    [Column("discount_rate", TypeName = "numeric(5,4)")]
    public decimal DiscountRate { get; set; }

    [Column("valid_from")]
    public DateTime? ValidFrom { get; set; }

    [Column("valid_to")]
    public DateTime? ValidTo { get; set; }

    [Column("usage_limit")]
    public int? UsageLimit { get; set; }

    [Required]
    [Column("used_count")]
    public int UsedCount { get; set; }
}
