import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  AppBar,
  Toolbar,
  Typography,
  Container,
  Grid,
  Card,
  CardContent,
  Box,
  Chip,
  Alert,
  CircularProgress,
  TextField,
  Button,
  Stack,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TableContainer,
  Paper,
  IconButton,
  Snackbar
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';

const USER_SERVICE_URL = process.env.REACT_APP_USER_SERVICE_URL || 'http://localhost:8081';
const ORDER_SERVICE_URL = process.env.REACT_APP_ORDER_SERVICE_URL || 'http://localhost:8082';
const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

function App() {
  const [services, setServices] = useState([]);
  const [anomalies, setAnomalies] = useState([]);
  const [metrics, setMetrics] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [users, setUsers] = useState([]);
  const [orders, setOrders] = useState([]);
  const [userLoading, setUserLoading] = useState(false);
  const [orderLoading, setOrderLoading] = useState(false);

  const [userForm, setUserForm] = useState({
    username: '',
    email: '',
    fullName: ''
  });
  const [orderForm, setOrderForm] = useState({
    userId: '',
    productName: '',
    quantity: 1,
    price: '',
    shippingAddress: ''
  });
  const [notification, setNotification] = useState({
    open: false,
    message: '',
    severity: 'success'
  });

  useEffect(() => {
    fetchObservability();
    fetchUsers();
    fetchOrders();
    const interval = setInterval(fetchObservability, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchObservability = async () => {
    try {
      setLoading(true);
      setError(null);

      let userServiceStatus = 'unavailable';
      let orderServiceStatus = 'unavailable';

      try {
        const userHealth = await axios.get(`${USER_SERVICE_URL}/health`, { timeout: 5000 });
        userServiceStatus = userHealth.data.includes('healthy') ? 'healthy' : 'unhealthy';
      } catch {
        userServiceStatus = 'unavailable';
      }

      try {
        const orderHealth = await axios.get(`${ORDER_SERVICE_URL}/health`, { timeout: 5000 });
        orderServiceStatus = orderHealth.data.includes('healthy') ? 'healthy' : 'unhealthy';
      } catch {
        orderServiceStatus = 'unavailable';
      }

      setServices([
        { name: 'User Service', status: userServiceStatus, port: 8081, url: USER_SERVICE_URL },
        { name: 'Order Service', status: orderServiceStatus, port: 8082, url: ORDER_SERVICE_URL }
      ]);

      const mockMetrics = {
        cpuUsage: [
          { time: '10:00', user: 45, order: 52 },
          { time: '10:05', user: 48, order: 55 },
          { time: '10:10', user: 42, order: 48 },
          { time: '10:15', user: 50, order: 60 },
          { time: '10:20', user: 47, order: 53 }
        ],
        memoryUsage: [
          { time: '10:00', user: 256, order: 312 },
          { time: '10:05', user: 268, order: 325 },
          { time: '10:10', user: 245, order: 298 },
          { time: '10:15', user: 275, order: 340 },
          { time: '10:20', user: 260, order: 315 }
        ],
        responseTime: [
          { time: '10:00', user: 120, order: 95 },
          { time: '10:05', user: 135, order: 110 },
          { time: '10:10', user: 98, order: 85 },
          { time: '10:15', user: 150, order: 125 },
          { time: '10:20', user: 115, order: 100 }
        ]
      };

      setMetrics(mockMetrics);

      const mockAnomalies = [
        {
          id: 1,
          service: 'User Service',
          type: 'High CPU Usage',
          severity: 'warning',
          timestamp: new Date().toISOString(),
          description: 'CPU usage exceeded 80% for 5 minutes'
        },
        {
          id: 2,
          service: 'Order Service',
          type: 'Memory Leak',
          severity: 'critical',
          timestamp: new Date(Date.now() - 300000).toISOString(),
          description: 'Memory usage continuously increasing'
        }
      ];

      setAnomalies(mockAnomalies);
      setLoading(false);
    } catch (err) {
      if (!err.message.includes('Network Error') && !err.message.includes('timeout')) {
        setError('Failed to fetch data: ' + err.message);
      }
      setLoading(false);
    }
  };

  const fetchUsers = async () => {
    try {
      setUserLoading(true);
      const response = await axios.get(`${USER_SERVICE_URL}/api/users`);
      setUsers(response.data);
    } catch (err) {
      showNotification('Failed to load users', 'error');
    } finally {
      setUserLoading(false);
    }
  };

  const fetchOrders = async () => {
    try {
      setOrderLoading(true);
      const response = await axios.get(`${ORDER_SERVICE_URL}/api/orders`);
      setOrders(response.data);
    } catch (err) {
      showNotification('Failed to load orders', 'error');
    } finally {
      setOrderLoading(false);
    }
  };

  const showNotification = (message, severity = 'success') => {
    setNotification({ open: true, message, severity });
  };

  const closeNotification = () => {
    setNotification((prev) => ({ ...prev, open: false }));
  };

  const handleUserInputChange = (event) => {
    const { name, value } = event.target;
    setUserForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleOrderInputChange = (event) => {
    const { name, value } = event.target;
    setOrderForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreateUser = async (event) => {
    event.preventDefault();
    if (!userForm.username || !userForm.email || !userForm.fullName) {
      showNotification('Please fill out all user fields', 'warning');
      return;
    }
    try {
      await axios.post(`${USER_SERVICE_URL}/api/users`, userForm);
      setUserForm({ username: '', email: '', fullName: '' });
      showNotification('User created successfully');
      fetchUsers();
    } catch (err) {
      const message = err.response?.data?.error || 'Failed to create user';
      showNotification(message, 'error');
    }
  };

  const handleDeleteUser = async (id) => {
    try {
      await axios.delete(`${USER_SERVICE_URL}/api/users/${id}`);
      showNotification('User deleted successfully', 'info');
      fetchUsers();
    } catch (err) {
      showNotification('Failed to delete user', 'error');
    }
  };

  const handleCreateOrder = async (event) => {
    event.preventDefault();
    if (!orderForm.userId || !orderForm.productName || !orderForm.quantity || !orderForm.price) {
      showNotification('Please fill out all order fields', 'warning');
      return;
    }

    const payload = {
      userId: Number(orderForm.userId),
      productName: orderForm.productName,
      quantity: Number(orderForm.quantity),
      price: parseFloat(orderForm.price),
      shippingAddress: orderForm.shippingAddress
    };

    if (Number.isNaN(payload.price) || payload.price <= 0) {
      showNotification('Price must be a positive number', 'warning');
      return;
    }

    try {
      await axios.post(`${ORDER_SERVICE_URL}/api/orders`, payload);
      setOrderForm({
        userId: '',
        productName: '',
        quantity: 1,
        price: '',
        shippingAddress: ''
      });
      showNotification('Order created successfully');
      fetchOrders();
    } catch (err) {
      const message = err.response?.data?.error || 'Failed to create order';
      showNotification(message, 'error');
    }
  };

  const handleCancelOrder = async (id) => {
    try {
      await axios.delete(`${ORDER_SERVICE_URL}/api/orders/${id}`);
      showNotification('Order cancelled successfully', 'info');
      fetchOrders();
    } catch (err) {
      showNotification('Failed to cancel order', 'error');
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'healthy':
        return 'success';
      case 'unhealthy':
        return 'error';
      case 'unavailable':
        return 'warning';
      default:
        return 'default';
    }
  };

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'critical':
        return 'error';
      case 'warning':
        return 'warning';
      case 'info':
        return 'info';
      default:
        return 'default';
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <div className="App">
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Cloud Native Observability Dashboard
          </Typography>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Typography variant="h5" gutterBottom>
              User Management
            </Typography>
          </Grid>

          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Create User
                </Typography>
                <Box component="form" onSubmit={handleCreateUser}>
                  <Stack spacing={2}>
                    <TextField
                      label="Username"
                      name="username"
                      value={userForm.username}
                      onChange={handleUserInputChange}
                      fullWidth
                      required
                    />
                    <TextField
                      label="Email"
                      name="email"
                      value={userForm.email}
                      onChange={handleUserInputChange}
                      type="email"
                      fullWidth
                      required
                    />
                    <TextField
                      label="Full Name"
                      name="fullName"
                      value={userForm.fullName}
                      onChange={handleUserInputChange}
                      fullWidth
                      required
                    />
                    <Button type="submit" variant="contained">
                      Create User
                    </Button>
                  </Stack>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={8}>
            <Card>
              <CardContent>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Typography variant="h6">Users</Typography>
                  {userLoading && <CircularProgress size={20} />}
                </Box>
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>ID</TableCell>
                        <TableCell>Username</TableCell>
                        <TableCell>Email</TableCell>
                        <TableCell>Full Name</TableCell>
                        <TableCell>Active</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {users.map((user) => (
                        <TableRow key={user.id}>
                          <TableCell>{user.id}</TableCell>
                          <TableCell>{user.username}</TableCell>
                          <TableCell>{user.email}</TableCell>
                          <TableCell>{user.fullName}</TableCell>
                          <TableCell>
                            <Chip
                              size="small"
                              label={user.active ? 'Active' : 'Inactive'}
                              color={user.active ? 'success' : 'default'}
                              variant="outlined"
                            />
                          </TableCell>
                          <TableCell align="right">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => handleDeleteUser(user.id)}
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12}>
            <Divider sx={{ my: 2 }} />
          </Grid>

          <Grid item xs={12}>
            <Typography variant="h5" gutterBottom>
              Order Management
            </Typography>
          </Grid>

          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Create Order
                </Typography>
                <Box component="form" onSubmit={handleCreateOrder}>
                  <Stack spacing={2}>
                    <TextField
                      label="User ID"
                      name="userId"
                      value={orderForm.userId}
                      onChange={handleOrderInputChange}
                      type="number"
                      inputProps={{ min: 1 }}
                      fullWidth
                      required
                    />
                    <TextField
                      label="Product Name"
                      name="productName"
                      value={orderForm.productName}
                      onChange={handleOrderInputChange}
                      fullWidth
                      required
                    />
                    <TextField
                      label="Quantity"
                      name="quantity"
                      value={orderForm.quantity}
                      onChange={handleOrderInputChange}
                      type="number"
                      inputProps={{ min: 1 }}
                      fullWidth
                      required
                    />
                    <TextField
                      label="Price"
                      name="price"
                      value={orderForm.price}
                      onChange={handleOrderInputChange}
                      type="number"
                      inputProps={{ min: 0, step: 0.01 }}
                      fullWidth
                      required
                    />
                    <TextField
                      label="Shipping Address"
                      name="shippingAddress"
                      value={orderForm.shippingAddress}
                      onChange={handleOrderInputChange}
                      fullWidth
                      multiline
                      minRows={2}
                    />
                    <Button type="submit" variant="contained">
                      Create Order
                    </Button>
                  </Stack>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={8}>
            <Card>
              <CardContent>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                  <Typography variant="h6">Orders</Typography>
                  {orderLoading && <CircularProgress size={20} />}
                </Box>
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>ID</TableCell>
                        <TableCell>Product</TableCell>
                        <TableCell>User</TableCell>
                        <TableCell align="right">Quantity</TableCell>
                        <TableCell align="right">Total</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {orders.map((order) => {
                        const total =
                          order.totalAmount !== undefined && order.totalAmount !== null
                            ? Number(order.totalAmount)
                            : null;
                        return (
                          <TableRow key={order.id}>
                            <TableCell>{order.id}</TableCell>
                            <TableCell>{order.productName}</TableCell>
                            <TableCell>{order.userId}</TableCell>
                            <TableCell align="right">{order.quantity}</TableCell>
                            <TableCell align="right">
                              {total === null
                                ? '-'
                                : new Intl.NumberFormat('en-US', {
                                    style: 'currency',
                                    currency: 'USD'
                                  }).format(total)}
                            </TableCell>
                            <TableCell>
                              <Chip
                                size="small"
                                label={order.status}
                                color={
                                  order.status === 'DELIVERED'
                                    ? 'success'
                                    : order.status === 'CANCELLED'
                                      ? 'default'
                                      : 'warning'
                                }
                                variant="outlined"
                              />
                            </TableCell>
                            <TableCell align="right">
                              <IconButton
                                size="small"
                                color="error"
                                onClick={() => handleCancelOrder(order.id)}
                              >
                                <DeleteIcon fontSize="small" />
                              </IconButton>
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              </CardContent>
            </Card>
          </Grid>


          <Grid item xs={12}>
            <Divider sx={{ my: 2 }} />
          </Grid>

          <Grid item xs={12}>
            <Typography variant="h5" gutterBottom>
              Service Status
            </Typography>
          </Grid>

          {services.map((service) => (
            <Grid item xs={12} md={6} key={service.name}>
              <Card>
                <CardContent>
                  <Box display="flex" justifyContent="space-between" alignItems="center">
                    <Typography variant="h6">{service.name}</Typography>
                    <Chip
                      label={service.status}
                      color={getStatusColor(service.status)}
                      variant="outlined"
                    />
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    Port: {service.port}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}

          <Grid item xs={12}>
            <Typography variant="h5" gutterBottom sx={{ mt: 3 }}>
              Performance Metrics
            </Typography>
          </Grid>

          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  CPU Usage (%)
                </Typography>
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={metrics.cpuUsage}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="time" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="user" stroke="#8884d8" name="User Service" />
                    <Line type="monotone" dataKey="order" stroke="#82ca9d" name="Order Service" />
                  </LineChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Memory Usage (MB)
                </Typography>
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={metrics.memoryUsage}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="time" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="user" stroke="#8884d8" name="User Service" />
                    <Line type="monotone" dataKey="order" stroke="#82ca9d" name="Order Service" />
                  </LineChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Response Time (ms)
                </Typography>
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={metrics.responseTime}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="time" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="user" stroke="#8884d8" name="User Service" />
                    <Line type="monotone" dataKey="order" stroke="#82ca9d" name="Order Service" />
                  </LineChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12}>
            <Typography variant="h5" gutterBottom sx={{ mt: 3 }}>
              AI Anomaly Detection
            </Typography>
          </Grid>

          {anomalies.map((anomaly) => (
            <Grid item xs={12} md={6} key={anomaly.id}>
              <Card>
                <CardContent>
                  <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="h6">{anomaly.service}</Typography>
                    <Chip
                      label={anomaly.type}
                      color={getSeverityColor(anomaly.severity)}
                      variant="outlined"
                    />
                  </Box>
                  <Typography variant="body2" color="text.secondary" mb={1}>
                    {anomaly.description}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {new Date(anomaly.timestamp).toLocaleString()}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>

      <Snackbar
        open={notification.open}
        autoHideDuration={4000}
        onClose={closeNotification}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={closeNotification}
          severity={notification.severity}
          sx={{ width: '100%' }}
        >
          {notification.message}
        </Alert>
      </Snackbar>
    </div>
  );
}

export default App;
