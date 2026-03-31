import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { getOrder } from '../services/order-service';

function OrderDetails() {
  const { orderNumber } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadOrderDetails = useCallback(async () => {
    if (!orderNumber) {
      setError('No order number provided');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    const result = await getOrder(orderNumber);
    if (result.success) {
      setOrder(result.data);
      setError(null);
    } else {
      setError(result.error.message);
    }
    setIsLoading(false);
  }, [orderNumber]);

  useEffect(() => {
    loadOrderDetails();
  }, [loadOrderDetails]);

  return (
    <>
      <Navbar title="Order Details" />
      <div className="container mt-4">
        <nav aria-label="breadcrumb">
          <ol className="breadcrumb">
            <li className="breadcrumb-item"><a href="/">Home</a></li>
            <li className="breadcrumb-item"><a href="/order-history">Order History</a></li>
            <li className="breadcrumb-item active">Order Details</li>
          </ol>
        </nav>

        <div className="card shadow">
          <div className="card-header bg-primary text-white">
            <h4 className="mb-0">Order Details</h4>
          </div>
          <div className="card-body">
            {isLoading ? (
              <div className="text-center p-4">
                <div className="spinner-border" role="status">
                  <span className="visually-hidden">Loading...</span>
                </div>
                <p className="mt-2">Loading order details...</p>
              </div>
            ) : error ? (
              <div className="alert alert-danger" role="alert">{error}</div>
            ) : order ? (
              <>
                <div className="row">
                  <div className="col-md-6 mb-3">
                    <strong>Order Number:</strong>
                    <p aria-label="Display Order Number">{order.orderNumber}</p>
                  </div>
                  <div className="col-md-6 mb-3">
                    <strong>Order Timestamp:</strong>
                    <p aria-label="Display Order Timestamp">
                      {new Date(order.orderTimestamp).toLocaleString('en-US', { timeZone: 'UTC' })}
                    </p>
                  </div>
                  <div className="col-md-6 mb-3">
                    <strong>Status:</strong>
                    <p className={`status-${order.status}`} aria-label="Display Status">
                      {order.status}
                    </p>
                  </div>
                  <div className="col-md-6 mb-3">
                    <strong>SKU:</strong>
                    <p aria-label="Display SKU">{order.sku}</p>
                  </div>
                  <div className="col-md-6 mb-3">
                    <strong>Quantity:</strong>
                    <p aria-label="Display Quantity">{order.quantity}</p>
                  </div>
                  <div className="col-md-6 mb-3">
                    <strong>Unit Price:</strong>
                    <p aria-label="Display Unit Price">${order.unitPrice.toFixed(2)}</p>
                  </div>
                  <div className="col-md-6 mb-3">
                    <strong>Total Price:</strong>
                    <p className="fs-5 fw-bold" aria-label="Display Total Price">
                      ${order.totalPrice.toFixed(2)}
                    </p>
                  </div>
                </div>

                <div className="mt-3">
                  <button
                    className="btn btn-secondary"
                    onClick={() => navigate('/order-history')}
                  >
                    Back to Order History
                  </button>
                </div>
              </>
            ) : (
              <p>Order not found</p>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

export default OrderDetails;
