# WebSocket Service

Real-time WebSocket service for mobile chat application.

## Setup

```bash
cd websocket-service
npm install
npm start
```

## Environment Variables

Create `.env` file:
```
PORT=3000
NODE_ENV=development
LOG_LEVEL=info
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080,http://localhost:8082
```

## Features

- Real-time messaging (public & private)
- User connection management
- Typing indicators
- Status updates
- Message history/queue
- Multiple rooms support

## API Endpoints

- `GET /health` - Health check
- `GET /stats` - Server statistics

## Socket Events

### Client sends:
- `join` - User join room
- `message` - Send public message
- `private-message` - Send private message
- `typing` - User typing indicator
- `stop-typing` - User stop typing
- `status-change` - Update user status
- `get-history` - Get message history
- `disconnect` - User disconnect

### Server sends:
- `welcome` - Connection welcome
- `online-users` - Online users list
- `message` - Public message received
- `private-message` - Private message received
- `user-typing` - User typing
- `user-stop-typing` - User stop typing
- `user-status-changed` - Status changed
- `user-joined` - User joined room
- `user-left` - User left room
- `message-history` - Message history

## Client Usage

```javascript
const socket = io('http://localhost:3000');

socket.on('connect', () => {
  socket.emit('join', {
    userId: 1,
    username: 'TestUser'
  });
});

socket.on('message', (msg) => {
  console.log(msg);
});

socket.emit('message', {
  senderId: 1,
  senderName: 'TestUser',
  content: 'Hello World'
});
```
