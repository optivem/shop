import { i18n } from '../../i18n';
import type { OrderFormData } from '../../types/form.types';

export interface ValidationError {
  field: string;
  message: string;
}

export function validateOrderForm(data: OrderFormData): ValidationError[] {
  const errors: ValidationError[] = [];
  const quantityTrimmed = data.quantityValue.trim();

  if (!data.sku) {
    errors.push({ field: 'sku', message: i18n.t('checkout:validation.skuEmpty') });
  }

  if (quantityTrimmed === '') {
    errors.push({ field: 'quantity', message: i18n.t('checkout:validation.quantityEmpty') });
  } else {
    const quantityNum = Number.parseFloat(quantityTrimmed);

    if (Number.isNaN(quantityNum)) {
      errors.push({ field: 'quantity', message: i18n.t('checkout:validation.quantityInteger') });
    } else if (!Number.isInteger(quantityNum)) {
      errors.push({ field: 'quantity', message: i18n.t('checkout:validation.quantityInteger') });
    } else if (quantityNum <= 0) {
      errors.push({ field: 'quantity', message: i18n.t('checkout:validation.quantityPositive') });
    }
  }

  if (!data.country) {
    errors.push({ field: 'country', message: i18n.t('checkout:validation.countryEmpty') });
  }

  return errors;
}
