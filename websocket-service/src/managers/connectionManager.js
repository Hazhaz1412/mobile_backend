const logger = require('../config/logger');

class ConnectionManager {
  constructor() {
    this.users = new Map();      // userId -> { socket, username, ... }
    this.rooms = new Map();      // roomId -> Set of userIds
  }

  /**
   * Handle user connect
   */
  addUser(userId, socket, username) {
    this.users.set(userId, {
      socketId: socket.id,
      userId: userId,
      username: username,
      connectedAt: new Date(),
      status: 'online'
    });
    
    logger.info(`User ${userId} (${username}) connected`);
    return this.users.get(userId);
  }

  /**
   * Handle user disconnect
   */
  removeUser(userId) {
    const user = this.users.get(userId);
    if (user) {
      this.users.delete(userId);
      logger.info(`User ${userId} disconnected`);
    }
    return user;
  }

  /**
   * Get user by ID
   */
  getUser(userId) {
    return this.users.get(userId);
  }

  /**
   * Get all online users
   */
  getOnlineUsers() {
    return Array.from(this.users.values()).map(user => ({
      userId: user.userId,
      username: user.username,
      status: user.status,
      connectedAt: user.connectedAt
    }));
  }

  /**
   * Check if user is online
   */
  isUserOnline(userId) {
    return this.users.has(userId);
  }

  /**
   * Update user status
   */
  updateUserStatus(userId, status) {
    const user = this.users.get(userId);
    if (user) {
      user.status = status;
      logger.info(`User ${userId} status: ${status}`);
    }
    return user;
  }

  /**
   * Add user to room
   */
  joinRoom(roomId, userId) {
    if (!this.rooms.has(roomId)) {
      this.rooms.set(roomId, new Set());
    }
    this.rooms.get(roomId).add(userId);
    logger.info(`User ${userId} joined room ${roomId}`);
  }

  /**
   * Remove user from room
   */
  leaveRoom(roomId, userId) {
    const room = this.rooms.get(roomId);
    if (room) {
      room.delete(userId);
      if (room.size === 0) {
        this.rooms.delete(roomId);
      }
      logger.info(`User ${userId} left room ${roomId}`);
    }
  }

  /**
   * Get users in room
   */
  getRoomUsers(roomId) {
    const room = this.rooms.get(roomId);
    if (!room) return [];
    
    return Array.from(room).map(userId => this.getUser(userId)).filter(Boolean);
  }

  /**
   * Get all rooms
   */
  getRooms() {
    return Array.from(this.rooms.keys());
  }

  /**
   * Clear all data (for testing)
   */
  clear() {
    this.users.clear();
    this.rooms.clear();
  }
}

module.exports = new ConnectionManager();
