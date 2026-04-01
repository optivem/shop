using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Optivem.Starter.Monolith.Core.Entities;

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
    [Column("sku")]
    public string Sku { get; set; } = null!;

    [Required]
    [Column("quantity")]
    public int Quantity { get; set; }

    [Required]
    [Column("unit_price", TypeName = "numeric(10,2)")]
    public decimal UnitPrice { get; set; }

    [Required]
    [Column("total_price", TypeName = "numeric(10,2)")]
    public decimal TotalPrice { get; set; }

    [Required]
    [Column("status")]
    public OrderStatus Status { get; set; }
}
