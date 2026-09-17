import { useState } from 'react';
import { orderService } from '../services/order-service';
import { validateOrderForm } from '../features/orders/order-validation';
import type { OrderFormData } from '../types/form.types';
import type { PlaceOrderResponse } from '../types/api.types';
import type { Result } from '../types/result.types';

const INITIAL_FORM: OrderFormData = {
  sku: '',
  quantity: 0,
  quantityValue: '',
  country: 'US',
  couponCode: '',
};

/**
 * Custom hook for managing order form state, validation, and submission
 * Handles all business logic for placing orders including client-side validation
 * @returns Form state, submission state, and control functions
 */
export function useOrderForm() {
  const [formData, setFormData] = useState<OrderFormData>(INITIAL_FORM);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submitOrder = async (): Promise<Result<PlaceOrderResponse>> => {
    const validationErrors = validateOrderForm(formData);
    if (validationErrors.length > 0) {
      const apiError = {
        message: 'The request contains one or more validation errors',
        fieldErrors: validationErrors.map(e => `${e.field}: ${e.message}`)
      };
      return {
        success: false,
        error: apiError
      };
    }

    setIsSubmitting(true);
    const couponCode = formData.couponCode.trim() || undefined;
    const result = await orderService.placeOrder(
      formData.sku,
      formData.quantity,
      formData.country,
      couponCode,
    );
    setIsSubmitting(false);

    if (result.success) {
      // Reset form on success
      setFormData(INITIAL_FORM);
    }

    return result;
  };

  const updateFormData = (updates: Partial<OrderFormData>) => {
    setFormData(prev => ({ ...prev, ...updates }));
  };

  const resetForm = () => {
    setFormData(INITIAL_FORM);
  };

  return {
    formData,
    updateFormData,
    isSubmitting,
    submitOrder,
    resetForm
  };
}
