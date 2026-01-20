/**
 * WebSocket Client Configuration
 * Connect to Node.js Socket.io server
 */

class WebSocketClient {
    constructor(config = {}) {
        this.config = {
            url: config.url || 'http://localhost:3000',
            userId: config.userId,
            username: config.username,
            onConnect: config.onConnect || (() => {}),
            onDisconnect: config.onDisconnect || (() => {}),
            onMessage: config.onMessage || (() => {}),
            onError: config.onError || (() => {}),
            ...config
        };
        
        this.socket = null;
        this.isConnected = false;
    }

    connect() {
        try {
            // Create socket connection
            this.socket = io(this.config.url, {
                reconnection: true,
                reconnectionDelay: 1000,
                reconnectionDelayMax: 5000,
                reconnectionAttempts: 5
            });

            // Connection events
            this.socket.on('connect', () => this._onConnect());
            this.socket.on('disconnect', () => this._onDisconnect());
            this.socket.on('error', (error) => this._onError(error));

            // Message events
            this.socket.on('welcome', (data) => {
                console.log('✓ Welcome:', data);
            });

            this.socket.on('message', (message) => {
                this.config.onMessage({
                    type: 'public',
                    data: message
                });
            });

            this.socket.on('private-message', (message) => {
                this.config.onMessage({
                    type: 'private',
                    data: message
                });
            });

            this.socket.on('online-users', (data) => {
                this.config.onMessage({
                    type: 'online-users',
                    data: data
                });
            });

            this.socket.on('user-joined', (data) => {
                this.config.onMessage({
                    type: 'user-joined',
                    data: data
                });
            });

            this.socket.on('user-left', (data) => {
                this.config.onMessage({
                    type: 'user-left',
                    data: data
                });
            });

            this.socket.on('user-typing', (data) => {
                this.config.onMessage({
                    type: 'user-typing',
                    data: data
                });
            });

            this.socket.on('user-stop-typing', (data) => {
                this.config.onMessage({
                    type: 'user-stop-typing',
                    data: data
                });
            });

            this.socket.on('user-status-changed', (data) => {
                this.config.onMessage({
                    type: 'user-status-changed',
                    data: data
                });
            });

            this.socket.on('message-history', (data) => {
                this.config.onMessage({
                    type: 'message-history',
                    data: data
                });
            });

        } catch (error) {
            console.error('Connection error:', error);
            this.config.onError(error);
        }
    }

    _onConnect() {
        this.isConnected = true;
        console.log('✓ Connected to WebSocket server');

        // Send join event
        this.socket.emit('join', {
            userId: this.config.userId,
            username: this.config.username
        });

        this.config.onConnect();
    }

    _onDisconnect() {
        this.isConnected = false;
        console.log('✗ Disconnected from server');
        this.config.onDisconnect();
    }

    _onError(error) {
        console.error('Socket error:', error);
        this.config.onError(error);
    }

    disconnect() {
        if (this.socket) {
            this.socket.disconnect();
        }
    }

    /**
     * Send public message
     */
    sendMessage(content) {
        if (!this._validateConnection()) return;

        this.socket.emit('message', {
            senderId: this.config.userId,
            senderName: this.config.username,
            content: content
        });
    }

    /**
     * Send private message
     */
    sendPrivateMessage(recipientId, recipientName, content) {
        if (!this._validateConnection()) return;

        this.socket.emit('private-message', {
            senderId: this.config.userId,
            senderName: this.config.username,
            recipientId: recipientId,
            recipientName: recipientName,
            content: content
        });
    }

    /**
     * Send typing indicator
     */
    sendTyping(recipientId = null) {
        if (!this._validateConnection()) return;

        this.socket.emit('typing', {
            userId: this.config.userId,
            username: this.config.username,
            recipientId: recipientId
        });
    }

    /**
     * Send stop typing indicator
     */
    sendStopTyping(recipientId = null) {
        if (!this._validateConnection()) return;

        this.socket.emit('stop-typing', {
            userId: this.config.userId,
            recipientId: recipientId
        });
    }

    /**
     * Update user status
     */
    updateStatus(status, message = '') {
        if (!this._validateConnection()) return;

        this.socket.emit('status-change', {
            userId: this.config.userId,
            status: status,
            message: message
        });
    }

    /**
     * Get message history
     */
    getHistory(afterTimestamp = null, limit = 50) {
        if (!this._validateConnection()) return;

        this.socket.emit('get-history', {
            after: afterTimestamp,
            limit: limit
        });
    }

    /**
     * Validate connection
     */
    _validateConnection() {
        if (!this.socket || !this.isConnected) {
            console.warn('WebSocket not connected');
            return false;
        }
        return true;
    }
}

// Export for use
if (typeof module !== 'undefined' && module.exports) {
    module.exports = WebSocketClient;
}
