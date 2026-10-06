using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace MyCompany.MyShop.Monolith.Core.Entities;

[Table("orders")]
public class Order
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    [Column("id")]
    public long Id { get; set; }

    [Required]
    [MaxLength(255)]
    [Column("order_number")]
    public string OrderNumber { get; set; } = null!;

    [Required]
    [Column("order_timestamp")]
    public DateTime OrderTimestamp { get; set; }

    [Required]
    [MaxLength(255)]
    [Column("country")]
    public string Country { get; set; } = null!;

    [Required]
    [MaxLength(255)]
    [Column("sku")]
    public string Sku { get; set; } = null!;

    [Required]
    [Column("quantity")]
    public int Quantity { get; set; }

    [Required]
    [Column("unit_price", TypeName = "numeric(10,2)")]
    public decimal UnitPrice { get; set; }

    [Required]
    [Column("base_price", TypeName = "numeric(10,2)")]
    public decimal BasePrice { get; set; }

    [Required]
    [Column("discount_rate", TypeName = "numeric(5,4)")]
    public decimal DiscountRate { get; set; }

    [Required]
    [Column("discount_amount", TypeName = "numeric(10,2)")]
    public decimal DiscountAmount { get; set; }

    [Required]
    [Column("subtotal_price", TypeName = "numeric(10,2)")]
    public decimal SubtotalPrice { get; set; }

    [Required]
    [Column("tax_rate", TypeName = "numeric(5,4)")]
    public decimal TaxRate { get; set; }

    [Required]
    [Column("tax_amount", TypeName = "numeric(10,2)")]
    public decimal TaxAmount { get; set; }

    [Required]
    [Column("total_price", TypeName = "numeric(10,2)")]
    public decimal TotalPrice { get; set; }

    [Required]
    [Column("status")]
    public OrderStatus Status { get; set; }

    [MaxLength(255)]
    [Column("applied_coupon_code")]
    public string? AppliedCouponCode { get; set; }

    /// <summary>Token subject of the customer who placed the order; null for orders that predate ownership.</summary>
    [MaxLength(255)]
    [Column("owner")]
    public string? Owner { get; set; }

    [MaxLength(255)]
    [Column("owner_name")]
    public string? OwnerName { get; set; }
}
