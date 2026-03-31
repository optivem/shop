import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { browseOrderHistory } from '../services/order-service';

function OrderHistory() {
  const [orders, setOrders] = useState([]);
  const [filter, setFilter] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadOrders = useCallback(async (filterValue) => {
    setIsLoading(true);
    setError(null);
    const result = await browseOrderHistory(filterValue);
    if (result.success) {
      setOrders(result.data.orders);
    } else {
      setError(result.error.message);
    }
    setIsLoading(false);
  }, []);

  useEffect(() => {
    loadOrders(filter);
  }, [filter, loadOrders]);

  const refresh = () => loadOrders(filter);

  return (
    <>
      <Navbar title="Order History" />
      <div className="container mt-4">
        <nav aria-label="breadcrumb">
          <ol className="breadcrumb">
            <li className="breadcrumb-item"><a href="/">Home</a></li>
            <li className="breadcrumb-item active">Order History</li>
          </ol>
        </nav>

        <div className="card shadow">
          <div className="card-header bg-primary text-white">
            <h4 className="mb-0">Order History</h4>
          </div>
          <div className="card-body">
            <div className="row mb-3">
              <div className="col-md-8">
                <label htmlFor="orderNumberFilter" className="form-label">
                  Filter by Order Number:
                </label>
                <input
                  type="text"
                  className="form-control"
                  id="orderNumberFilter"
                  aria-label="Order Number"
                  value={filter}
                  onChange={(e) => setFilter(e.target.value)}
                  placeholder="Enter order number..."
                />
              </div>
              <div className="col-md-4 d-flex align-items-end">
                <button
                  className="btn btn-secondary w-100"
                  onClick={refresh}
                  disabled={isLoading}
                  aria-label="Refresh Order List"
                >
                  Refresh
                </button>
              </div>
            </div>

            {isLoading ? (
              <div className="text-center p-4">
                <div className="spinner-border" role="status">
                  <span className="visually-hidden">Loading...</span>
                </div>
                <p className="mt-2">Loading orders...</p>
              </div>
            ) : error ? (
              <div className="alert alert-danger" role="alert">
                {error}
                <button className="btn btn-link" onClick={refresh}>Retry</button>
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-striped table-hover">
                  <thead>
                    <tr>
                      <th>Order Number</th>
                      <th>Order Date</th>
                      <th>SKU</th>
                      <th>Quantity</th>
                      <th>Total Price</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orders.length === 0 ? (
                      <tr>
                        <td colSpan="7" className="text-center">No orders found</td>
                      </tr>
                    ) : (
                      orders.map((order) => (
                        <tr key={order.orderNumber}>
                          <td>{order.orderNumber}</td>
                          <td>{new Date(order.orderTimestamp).toLocaleString('en-US', { timeZone: 'UTC' })}</td>
                          <td>{order.sku}</td>
                          <td>{order.quantity}</td>
                          <td>${order.totalPrice.toFixed(2)}</td>
                          <td>
                            <span className={`status-${order.status}`}>{order.status}</span>
                          </td>
                          <td>
                            <Link to={`/order-details/${encodeURIComponent(order.orderNumber)}`}>
                              View Details
                            </Link>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

export default OrderHistory;
