import { useState } from 'react';
import { orderService } from '../services/order-service';
import type { OrderFormData } from '../types/form.types';
import type { PlaceOrderResponse } from '../types/api.types';
import type { Result } from '../types/result.types';

interface ValidationError {
  field: string;
  message: string;
}

/**
 * Custom hook for managing order form state, validation, and submission
 * Handles all business logic for placing orders including client-side validation
 * @returns Form state, submission state, and control functions
 */
export function useOrderForm() {
  const [formData, setFormData] = useState<OrderFormData>({
    sku: '',
    quantity: 0,
    quantityValue: '',
    country: '',
    couponCode: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validateFormData = (data: OrderFormData): ValidationError[] => {
    const errors: ValidationError[] = [];
    const quantityTrimmed = data.quantityValue.trim();

    if (!data.sku) {
      errors.push({ field: 'sku', message: 'SKU must not be empty' });
    }

    if (quantityTrimmed === '') {
      errors.push({ field: 'quantity', message: 'Quantity must not be empty' });
    } else {
      const quantityNum = Number.parseFloat(quantityTrimmed);

      if (Number.isNaN(quantityNum)) {
        errors.push({ field: 'quantity', message: 'Quantity must be an integer' });
      } else if (!Number.isInteger(quantityNum)) {
        errors.push({ field: 'quantity', message: 'Quantity must be an integer' });
      } else if (quantityNum <= 0) {
        errors.push({ field: 'quantity', message: 'Quantity must be positive' });
      }
    }


    return errors;
  };

  const submitOrder = async (): Promise<Result<PlaceOrderResponse>> => {
    const validationErrors = validateFormData(formData);
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
      setFormData({
        sku: '',
        quantity: 0,
        quantityValue: '',
        country: '',
        couponCode: '',
      });
    }

    return result;
  };

  const updateFormData = (updates: Partial<OrderFormData>) => {
    setFormData(prev => ({ ...prev, ...updates }));
  };

  const resetForm = () => {
    setFormData({
      sku: '',
      quantity: 0,
      quantityValue: '',
      country: '',
      couponCode: '',
    });
  };

  return {
    formData,
    updateFormData,
    isSubmitting,
    submitOrder,
    resetForm
  };
}
