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

server.get('/erp/api', (req, res) => {
  res.status(200).json({
    message: 'ERP API',
    endpoints: ['/erp/api/products']
  });
});
server.use('/erp/api', erpRouter);

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
  console.log(`Clock Health: http://localhost:${port}/clock/health`);
  console.log(`Clock API: http://localhost:${port}/clock/api/time`);
});
