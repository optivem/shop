using System.Runtime.CompilerServices;
using Driver.Port.Dtos;
using Common.Domain;

namespace Dsl.Port.Then.Steps;

public interface IThenOrder
{
    IThenOrder HasSku(string expectedSku);

    IThenOrder HasQuantity(int expectedQuantity);

    IThenOrder HasCountry(string expectedCountry);

    IThenOrder HasUnitPrice(decimal expectedUnitPrice);

    IThenOrder HasBasePrice(decimal expectedBasePrice);

    IThenOrder HasBasePrice(string basePrice);

    IThenOrder HasSubtotalPrice(decimal expectedSubtotalPrice);

    IThenOrder HasSubtotalPrice(string expectedSubtotalPrice);

    IThenOrder HasTotalPrice(decimal expectedTotalPrice);

    IThenOrder HasTotalPrice(string expectedTotalPrice);

    IThenOrder HasStatus(OrderStatus expectedStatus);

    IThenOrder HasDiscountRateGreaterThanOrEqualToZero();

    IThenOrder HasDiscountRate(decimal expectedDiscountRate);

    IThenOrder HasDiscountAmount(decimal expectedDiscountAmount);

    IThenOrder HasDiscountAmount(string expectedDiscountAmount);

    IThenOrder HasAppliedCoupon(string expectedCouponCode);

    IThenOrder HasAppliedCoupon();

    IThenOrder HasDiscountAmountGreaterThanOrEqualToZero();

    IThenOrder HasSubtotalPriceGreaterThanZero();

    IThenOrder HasTaxRate(decimal expectedTaxRate);

    IThenOrder HasTaxRate(string expectedTaxRate);

    IThenOrder HasTaxRateGreaterThanOrEqualToZero();

    IThenOrder HasTaxAmount(string expectedTaxAmount);

    IThenOrder HasTaxAmountGreaterThanOrEqualToZero();

    IThenOrder HasTotalPriceGreaterThanZero();

    IThenOrder HasOrderNumberPrefix(string expectedPrefix);

    /// <summary>The order is the one known in this scenario by the given order-number alias.</summary>
    IThenOrder HasOrderNumber(string orderNumber);

    /// <summary>The order is not the one known in this scenario by the given order-number alias.</summary>
    IThenOrder DoesNotHaveOrderNumber(string orderNumber);

    /// <summary>The order ships to this address; the country is resolved like <see cref="HasCountry"/>.</summary>
    IThenOrder HasShippingAddress(string firstName, string lastName, string street, string postalCode, string city, string country);

    IThenOrder HasNoShippingAddress();

    TaskAwaiter GetAwaiter();
}
