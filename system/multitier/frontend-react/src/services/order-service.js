const BASE_URL = '/api/orders';

async function fetchJson(url, options) {
  try {
    const response = await fetch(url, options);

    if (response.ok) {
      if (response.status === 204) {
        return { success: true, data: undefined };
      }
      const data = await response.json();
      return { success: true, data };
    }

    let error;
    try {
      const errorData = await response.json();
      let fieldErrors;
      if (errorData.errors && Array.isArray(errorData.errors) && errorData.errors.length > 0) {
        fieldErrors = errorData.errors.map(e => `${e.field}: ${e.message}`);
      }
      error = {
        message: errorData.detail || `An unexpected error occurred. (Status: ${response.status})`,
        fieldErrors,
        status: response.status
      };
    } catch (e) {
      error = {
        message: `An unexpected error occurred. (Status: ${response.status})`,
        status: response.status
      };
    }
    return { success: false, error };
  } catch (e) {
    return {
      success: false,
      error: {
        message: `Network error: ${e.message}`,
        status: 0
      }
    };
  }
}

export async function placeOrder(sku, quantity) {
  return fetchJson(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sku, quantity })
  });
}

export async function getOrder(orderNumber) {
  return fetchJson(`${BASE_URL}/${orderNumber}`, { method: 'GET' });
}

export async function browseOrderHistory(orderNumberFilter) {
  const url = orderNumberFilter && orderNumberFilter.trim()
    ? `${BASE_URL}?orderNumber=${encodeURIComponent(orderNumberFilter.trim())}`
    : BASE_URL;
  return fetchJson(url, { method: 'GET' });
}
