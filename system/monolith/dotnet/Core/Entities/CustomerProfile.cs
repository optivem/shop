using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace MyCompany.MyShop.Monolith.Core.Entities;

/// <summary>A customer's name and default shipping address, one row per token subject. The email is never stored.</summary>
[Table("customer_profiles")]
public class CustomerProfile
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
    [Column("first_name")]
    public string FirstName { get; set; } = null!;

    [Required]
    [MaxLength(255)]
    [Column("last_name")]
    public string LastName { get; set; } = null!;

    [Required]
    [MaxLength(255)]
    [Column("street")]
    public string Street { get; set; } = null!;

    [Required]
    [MaxLength(255)]
    [Column("postal_code")]
    public string PostalCode { get; set; } = null!;

    [Required]
    [MaxLength(255)]
    [Column("city")]
    public string City { get; set; } = null!;

    [Required]
    [MaxLength(255)]
    [Column("country")]
    public string Country { get; set; } = null!;

    [Required]
    [Column("updated_at")]
    public DateTime UpdatedAt { get; set; }
}
