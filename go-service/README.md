# Go Chat & Video Service

Go microservice handling real-time chat messaging and live video streaming for the Mobile App.

## Features

- **gRPC Services** for Chat and Video
- **Scylla DB** (Cassandra-compatible) for message/call storage
- **Protocol Buffers** for efficient data serialization
- **WebSocket** support for real-time updates

## Project Structure

```
go-service/
├── cmd/
│   └── main.go                 # Entry point
├── internal/
│   ├── chat/
│   │   ├── service.go
│   │   └── service_test.go
│   └── video/
│       ├── service.go
│       └── service_test.go
├── api/
│   ├── chat/
│   │   └── chat.proto
│   └── video/
│       └── video.proto
├── go.mod
├── Dockerfile
└── README.md
```

## Getting Started

### Prerequisites
- Go 1.21+
- Scylla running (via podman-compose)

### Build
```bash
cd go-service
go build -o bin/mobile-chat-video ./cmd
```

### Run
```bash
./bin/mobile-chat-video
```

### Tests
```bash
go test ./...
```

## gRPC Services

### Chat Service
- `SendMessage(Message)` - Send a message
- `StreamMessages(StreamRequest)` - Stream messages in real-time
- `GetChatHistory(ChatHistoryRequest)` - Retrieve message history

### Video Service
- `InitiateCall(CallRequest)` - Start a call
- `EndCall(EndCallRequest)` - End a call
- `StreamICECandidates(ICECandidate)` - Stream ICE candidates for WebRTC
- `GetActiveStreams(GetStreamsRequest)` - Get active calls

## Environment Variables

Set in `.env`:
```
SCYLLA_HOST=scylla
SCYLLA_PORT=9042
GRPC_PORT=50051
```
