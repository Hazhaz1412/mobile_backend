const express = require('express');
const http = require('http');
const socketIO = require('socket.io');
const logger = require('./config/logger');
require('dotenv').config();

const { setupHandlers } = require('./handlers/socketHandler');

const app = express();
const server = http.createServer(app);
const io = socketIO(server, {
  cors: {
    origin: (process.env.ALLOWED_ORIGINS || 'http://localhost:8082').split(','),
    methods: ['GET', 'POST']
  },
  transports: ['websocket', 'polling'],
  maxHttpBufferSize: 1e6, // 1MB max message size
  pingInterval: 25000,
  pingTimeout: 60000
});

// Middleware
app.use(express.json());

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'OK',
    timestamp: new Date(),
    uptime: process.uptime()
  });
});

// Stats endpoint
app.get('/stats', (req, res) => {
  const connectionManager = require('./managers/connectionManager');
  const messageQueue = require('./managers/messageQueue');
  
  res.json({
    onlineUsers: connectionManager.getOnlineUsers().length,
    totalUsers: connectionManager.users.size,
    totalRooms: connectionManager.getRooms().length,
    queuedMessages: messageQueue.size(),
    timestamp: new Date()
  });
});

// Socket.io connection handler
io.on('connection', (socket) => {
  logger.info(`New connection: ${socket.id}`);
  
  // Setup event handlers
  setupHandlers(io, socket);
  
  // Send welcome message
  socket.emit('welcome', {
    message: 'Connected to WebSocket server',
    socketId: socket.id,
    timestamp: new Date()
  });
});

// Start server
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  logger.info('========================================');
  logger.info(`WebSocket Service listening on port ${PORT}`);
  logger.info(`Environment: ${process.env.NODE_ENV}`);
  logger.info(`Allowed Origins: ${process.env.ALLOWED_ORIGINS}`);
  logger.info('========================================');
});

// Graceful shutdown
process.on('SIGTERM', () => {
  logger.info('SIGTERM received, shutting down gracefully...');
  server.close(() => {
    logger.info('Server closed');
    process.exit(0);
  });
});

module.exports = { app, io, server };
