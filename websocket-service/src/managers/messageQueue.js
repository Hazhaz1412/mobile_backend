const { v4: uuidv4 } = require('uuid');
const logger = require('../config/logger');

class MessageQueue {
  constructor() {
    this.messages = [];
    this.maxSize = 1000; // Max messages in memory
  }

  /**
   * Add message to queue
   */
  enqueue(message) {
    const queuedMessage = {
      id: uuidv4(),
      ...message,
      queuedAt: new Date()
    };
    
    this.messages.push(queuedMessage);
    
    // Remove old messages if exceeded max size
    if (this.messages.length > this.maxSize) {
      this.messages.shift();
    }
    
    return queuedMessage;
  }

  /**
   * Get messages for user (after timestamp)
   */
  getMessages(afterTimestamp = null, limit = 50) {
    let filtered = this.messages;
    
    if (afterTimestamp) {
      filtered = filtered.filter(msg => 
        new Date(msg.queuedAt) > new Date(afterTimestamp)
      );
    }
    
    return filtered.slice(-limit);
  }

  /**
   * Clear queue
   */
  clear() {
    this.messages = [];
  }

  /**
   * Get queue size
   */
  size() {
    return this.messages.length;
  }
}

module.exports = new MessageQueue();
