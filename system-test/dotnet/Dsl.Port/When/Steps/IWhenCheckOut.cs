using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

/// <summary>
/// The customer checks out. The steps run in this order: put the cart lines in the cart (each from its product page,
/// in the order given); change the ERP; open the checkout (by its address, unless proceeding from the cart); enter the
/// shipping address and coupon; place the order when asked. SKUs and coupon codes are aliases; the shipping address
/// is literal, its country a code as the checkout's form offers it.
/// </summary>
public interface IWhenCheckOut : IWhenStep
{
    /// <summary>Before the checkout, the cart already contains this line.</summary>
    IWhenCheckOut WithCartContaining(string skuAlias, int quantity);

    /// <summary>After the cart is filled, the ERP's price of this product changes.</summary>
    IWhenCheckOut WithErpPriceChanged(string skuAlias, string unitPrice);

    /// <summary>After the cart is filled, the ERP no longer offers this product.</summary>
    IWhenCheckOut WithErpNoLongerOffering(string skuAlias);

    /// <summary>Proceeds to checkout from the cart instead of opening the checkout by its address.</summary>
    IWhenCheckOut WithProceededFromCart();

    /// <summary>Proceeds to checkout from the cart, and signs in as a customer when asked to.</summary>
    IWhenCheckOut WithProceededFromCartAndSignedIn();

    /// <summary>Chooses to ship to this address instead of the saved one (the form is open anyway on a first order).</summary>
    IWhenCheckOut WithShippingAddress(string firstName, string lastName, string street, string postalCode, string city,
        string country);

    IWhenCheckOut WithCouponCode(string couponCode);

    /// <summary>Places the order; without this, the checkout is only opened (and the address entered).</summary>
    IWhenCheckOut WithOrderPlaced();
}
