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

    TaskAwaiter GetAwaiter();
}
