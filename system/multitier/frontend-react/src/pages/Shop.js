import React, { useState } from 'react';
import Navbar from '../components/Navbar';
import { placeOrder } from '../services/order-service';

function Shop() {
  const [sku, setSku] = useState('');
  const [quantityValue, setQuantityValue] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notification, setNotification] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setNotification(null);

    // Client-side validation
    const errors = [];
    if (!sku.trim()) {
      errors.push('SKU must not be empty');
    }
    const trimmedQty = quantityValue.trim();
    if (trimmedQty === '') {
      errors.push('Quantity must not be empty');
    } else {
      const qty = parseFloat(trimmedQty);
      if (isNaN(qty)) {
        errors.push('Quantity must be an integer');
      } else if (!Number.isInteger(qty)) {
        errors.push('Quantity must be an integer');
      } else if (qty <= 0) {
        errors.push('Quantity must be positive');
      }
    }

    if (errors.length > 0) {
      setNotification({ type: 'error', message: errors.join('. ') });
      return;
    }

    setIsSubmitting(true);
    const result = await placeOrder(sku.trim(), parseInt(trimmedQty));
    setIsSubmitting(false);

    if (result.success) {
      setNotification({
        type: 'success',
        message: `Success! Order has been created with Order Number ${result.data.orderNumber}`
      });
      setSku('');
      setQuantityValue('');
    } else {
      let msg = result.error.message;
      if (result.error.fieldErrors) {
        msg += ' ' + result.error.fieldErrors.join('. ');
      }
      setNotification({ type: 'error', message: msg });
    }
  };

  return (
    <>
      <Navbar title="Shop" />
      <div className="container mt-4">
        <nav aria-label="breadcrumb">
          <ol className="breadcrumb">
            <li className="breadcrumb-item"><a href="/">Home</a></li>
            <li className="breadcrumb-item active">Shop</li>
          </ol>
        </nav>

        {notification && (
          <div className={`notification ${notification.type}`} role="alert">
            {notification.message}
          </div>
        )}

        <div className="row">
          <div className="col-lg-6 mx-auto">
            <div className="card shadow">
              <div className="card-header bg-primary text-white">
                <h4 className="mb-0">Place Your Order</h4>
              </div>
              <div className="card-body">
                <form onSubmit={handleSubmit}>
                  <div className="mb-3">
                    <label htmlFor="sku" className="form-label">SKU:</label>
                    <input
                      type="text"
                      className="form-control"
                      id="sku"
                      aria-label="SKU"
                      value={sku}
                      onChange={(e) => setSku(e.target.value)}
                      placeholder="Enter product SKU"
                    />
                  </div>
                  <div className="mb-3">
                    <label htmlFor="quantity" className="form-label">Quantity:</label>
                    <input
                      type="text"
                      className="form-control"
                      id="quantity"
                      aria-label="Quantity"
                      inputMode="numeric"
                      value={quantityValue}
                      onChange={(e) => setQuantityValue(e.target.value)}
                      placeholder="Enter quantity"
                    />
                  </div>
                  <div className="d-grid">
                    <button
                      type="submit"
                      className="btn btn-primary btn-lg"
                      disabled={isSubmitting}
                      aria-label="Place Order"
                    >
                      {isSubmitting ? 'Placing Order...' : 'Place Order'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

export default Shop;
