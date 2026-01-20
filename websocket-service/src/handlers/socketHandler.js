const logger = require('../config/logger');
const connectionManager = require('../managers/connectionManager');
const messageQueue = require('../managers/messageQueue');

const setupHandlers = (io, socket) => {
  /**
   * User join event
   */
  socket.on('join', (data) => {
    const { userId, username } = data;
    
    if (!userId || !username) {
      socket.emit('error', { message: 'userId and username required' });
      return;
    }
    
    // Add user to connection manager
    connectionManager.addUser(userId, socket, username);
    
    // Join to personal room
    socket.join(`user:${userId}`);
    
    // Join to public room
    socket.join('public');
    
    // Broadcast user joined
    io.to('public').emit('user-joined', {
      userId: userId,
      username: username,
      onlineUsers: connectionManager.getOnlineUsers()
    });
    
    // Send online users to this user
    socket.emit('online-users', {
      users: connectionManager.getOnlineUsers()
    });
    
    logger.info(`User ${userId} (${username}) joined`);
  });

  /**
   * Send public message
   */
  socket.on('message', (data) => {
    const { senderId, senderName, content } = data;
    
    if (!content) return;
    
    const message = {
      senderId,
      senderName,
      content,
      timestamp: new Date(),
      type: 'public'
    };
    
    // Queue message
    messageQueue.enqueue(message);
    
    // Broadcast to all users
    io.to('public').emit('message', message);
    
    logger.info(`[${senderName}]: ${content}`);
  });

  /**
   * Send private message
   */
  socket.on('private-message', (data) => {
    const { senderId, senderName, recipientId, recipientName, content } = data;
    
    if (!content || !recipientId) return;
    
    const message = {
      senderId,
      senderName,
      recipientId,
      recipientName,
      content,
      timestamp: new Date(),
      type: 'private'
    };
    
    // Queue message
    messageQueue.enqueue(message);
    
    // Send to recipient
    io.to(`user:${recipientId}`).emit('private-message', message);
    
    // Send confirmation to sender
    socket.emit('private-message-sent', {
      recipientId,
      recipientName,
      message
    });
    
    logger.info(`[PRIVATE] ${senderName} -> ${recipientName}: ${content}`);
  });

  /**
   * Typing indicator
   */
  socket.on('typing', (data) => {
    const { userId, username, recipientId } = data;
    
    if (recipientId) {
      // Send to specific user
      io.to(`user:${recipientId}`).emit('user-typing', {
        userId,
        username
      });
    } else {
      // Broadcast to all
      socket.broadcast.emit('user-typing', {
        userId,
        username
      });
    }
  });

  /**
   * Stop typing
   */
  socket.on('stop-typing', (data) => {
    const { userId, recipientId } = data;
    
    if (recipientId) {
      io.to(`user:${recipientId}`).emit('user-stop-typing', { userId });
    } else {
      socket.broadcast.emit('user-stop-typing', { userId });
    }
  });

  /**
   * Update status (online, away, offline)
   */
  socket.on('status-change', (data) => {
    const { userId, status } = data;
    
    connectionManager.updateUserStatus(userId, status);
    
    io.to('public').emit('user-status-changed', {
      userId,
      status,
      timestamp: new Date()
    });
  });

  /**
   * Get message history
   */
  socket.on('get-history', (data) => {
    const { after, limit } = data;
    const messages = messageQueue.getMessages(after, limit);
    
    socket.emit('message-history', { messages });
  });

  /**
   * User disconnect
   */
  socket.on('disconnect', () => {
    // Find user by socket ID
    let disconnectedUser = null;
    for (let [userId, userInfo] of Array.from(connectionManager.users)) {
      if (userInfo.socketId === socket.id) {
        disconnectedUser = connectionManager.removeUser(userId);
        break;
      }
    }
    
    if (disconnectedUser) {
      io.to('public').emit('user-left', {
        userId: disconnectedUser.userId,
        username: disconnectedUser.username,
        onlineUsers: connectionManager.getOnlineUsers()
      });
    }
  });
};

module.exports = { setupHandlers };
