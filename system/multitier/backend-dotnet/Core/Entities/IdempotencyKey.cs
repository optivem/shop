using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace MyCompany.MyShop.Backend.Core.Entities;

/// <summary>
/// One answered <c>POST /api/orders</c>, keyed by the caller and their <c>Idempotency-Key</c> header, so a
/// retry gets the stored response instead of a second order. Written in the same transaction as the order.
/// </summary>
[Table("idempotency_keys")]
public class IdempotencyKey
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    [Column("id")]
    public long Id { get; set; }

    [Required]
    [MaxLength(255)]
    [Column("owner")]
    public string Owner { get; set; } = null!;

    [Required]
    [MaxLength(255)]
    [Column("idempotency_key")]
    public string Key { get; set; } = null!;

    [Required]
    [MaxLength(64)]
    [Column("request_hash")]
    public string RequestHash { get; set; } = null!;

    [Required]
    [Column("response_status")]
    public int ResponseStatus { get; set; }

    [Required]
    [Column("response_body")]
    public string ResponseBody { get; set; } = null!;

    [Required]
    [Column("created_at")]
    public DateTime CreatedAt { get; set; }
}
