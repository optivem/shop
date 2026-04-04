using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Optivem.Shop.Monolith.Core.Entities;

[Table("orders")]
public class Order
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    [Column("id")]
    public long Id { get; set; }

    [Required]
    [Column("order_number")]
    public string OrderNumber { get; set; } = null!;

    [Required]
    [Column("order_timestamp")]
    public DateTime OrderTimestamp { get; set; }

    [Required]
    [Column("country")]
    public string Country { get; set; } = null!;

    [Required]
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

    [Column("applied_coupon_code")]
    public string? AppliedCouponCode { get; set; }
}
