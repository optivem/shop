import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import Todos from './pages/Todos';
import Shop from './pages/Shop';
import OrderHistory from './pages/OrderHistory';
import OrderDetails from './pages/OrderDetails';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/todos" element={<Todos />} />
        <Route path="/shop" element={<Shop />} />
        <Route path="/order-history" element={<OrderHistory />} />
        <Route path="/order-details/:orderNumber" element={<OrderDetails />} />
      </Routes>
    </Router>
  );
}

export default App;
