// Mock API server using json-server
// Serves ERP and Clock APIs under namespaced paths
const jsonServer = require('json-server');

const server = jsonServer.create();
const middlewares = jsonServer.defaults({
  logger: true,
  noCors: false
});

server.use(middlewares);
server.use(jsonServer.bodyParser);

// ERP subsystem health check
server.get('/erp/health', (req, res) => {
  res.status(200).json({
    status: 'UP',
    subsystem: 'ERP',
    timestamp: new Date().toISOString()
  });
});

// Tax subsystem health check
server.get('/tax/health', (req, res) => {
  res.status(200).json({
    status: 'UP',
    subsystem: 'Tax',
    timestamp: new Date().toISOString()
  });
});

// Clock subsystem health check
server.get('/clock/health', (req, res) => {
  res.status(200).json({
    status: 'UP',
    subsystem: 'Clock',
    timestamp: new Date().toISOString()
  });
});

// In-memory data for ERP API
const erpRouter = jsonServer.router({
  products: [
    {
      id: "HP-15",
      title: "HP Laptop 15",
      description: "15.6-inch laptop with Intel Core i5 processor",
      price: 699.99,
      category: "Laptops",
      brand: "HP"
    },
    {
      id: "DELL-XPS",
      title: "Dell XPS 13",
      description: "13.3-inch ultrabook with Intel Core i7 processor",
      price: 1299.99,
      category: "Laptops",
      brand: "Dell"
    },
    {
      id: "LENOVO-T14",
      title: "Lenovo ThinkPad T14",
      description: "14-inch business laptop",
      price: 999.99,
      category: "Laptops",
      brand: "Lenovo"
    }
  ]
});

// Coerce string prices to numbers on product creation
server.post('/erp/api/products', (req, res, next) => {
  if (req.body && typeof req.body.price === 'string') {
    req.body.price = parseFloat(req.body.price);
  }
  next();
});

// Promotion endpoint - returns default no-promotion state
server.get('/erp/api/promotion', (req, res) => {
  res.status(200).json({
    promotionActive: false,
    discount: 1.0
  });
});

server.get('/erp/api', (req, res) => {
  res.status(200).json({
    message: 'ERP API',
    endpoints: ['/erp/api/products', '/erp/api/promotion']
  });
});
server.use('/erp/api', erpRouter);

// In-memory data for Tax API
const taxRouter = jsonServer.router({
  countries: [
    {
      id: "US",
      countryName: "United States",
      taxRate: 0
    },
    {
      id: "GB",
      countryName: "United Kingdom",
      taxRate: 0.2
    },
    {
      id: "DE",
      countryName: "Germany",
      taxRate: 0.19
    },
    {
      id: "FR",
      countryName: "France",
      taxRate: 0.2
    },
    {
      id: "JP",
      countryName: "Japan",
      taxRate: 0.1
    }
  ]
});

server.get('/tax/api', (req, res) => {
  res.status(200).json({
    message: 'Tax API',
    endpoints: ['/tax/api/countries']
  });
});
server.use('/tax/api', taxRouter);

// Clock API - returns fixed timestamp
server.get('/clock/api/time', (req, res) => {
  res.status(200).json({
    time: '2024-01-15T10:30:00.000Z'
  });
});

const port = 9000;
server.listen(port, () => {
  console.log(`Mock API Server running on http://localhost:${port}`);
  console.log(`ERP Health: http://localhost:${port}/erp/health`);
  console.log(`ERP API: http://localhost:${port}/erp/api/products`);
  console.log(`Tax Health: http://localhost:${port}/tax/health`);
  console.log(`Tax API: http://localhost:${port}/tax/api/countries`);
  console.log(`Clock Health: http://localhost:${port}/clock/health`);
  console.log(`Clock API: http://localhost:${port}/clock/api/time`);
});
