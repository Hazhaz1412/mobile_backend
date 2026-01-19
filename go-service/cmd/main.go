package main

import (
	"fmt"
	"log"
	"net"

	"github.com/gocql/gocql"
	"google.golang.org/grpc"
)

func initScylla() (*gocql.Session, error) {
	cluster := gocql.NewCluster("scylla")
	cluster.Keyspace = "chat_keyspace"
	cluster.Port = 9042

	session, err := cluster.CreateSession()
	if err != nil {
		return nil, fmt.Errorf("failed to connect to Scylla: %w", err)
	}

	// Create keyspace if not exists
	if err := session.Query(`
		CREATE KEYSPACE IF NOT EXISTS chat_keyspace
		WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}
	`).Exec(); err != nil {
		log.Printf("Warning: could not create keyspace: %v", err)
	}

	// Create tables
	err = session.Query(`
		CREATE TABLE IF NOT EXISTS chat_keyspace.messages (
			id UUID PRIMARY KEY,
			sender_id UUID,
			receiver_id UUID,
			content TEXT,
			timestamp BIGINT,
			is_read BOOLEAN
		)
	`).Exec()
	if err != nil {
		log.Printf("Warning: could not create messages table: %v", err)
	}

	err = session.Query(`
		CREATE TABLE IF NOT EXISTS chat_keyspace.calls (
			call_id UUID PRIMARY KEY,
			caller_id UUID,
			callee_id UUID,
			call_type TEXT,
			status TEXT,
			start_time BIGINT,
			end_time BIGINT,
			duration BIGINT
		)
	`).Exec()
	if err != nil {
		log.Printf("Warning: could not create calls table: %v", err)
	}

	return session, nil
}

func main() {
	log.Println("Starting Go Chat & Video Service...")

	// Initialize Scylla
	session, err := initScylla()
	if err != nil {
		log.Fatalf("Failed to initialize Scylla: %v", err)
	}
	defer session.Close()

	log.Println("Scylla connected successfully")

	// Start gRPC server
	listener, err := net.Listen("tcp", ":50051")
	if err != nil {
		log.Fatalf("Failed to listen on port 50051: %v", err)
	}

	server := grpc.NewServer()

	// Register services (will implement next)
	// pb.RegisterChatServiceServer(server, &chatService{session: session})
	// pb.RegisterVideoServiceServer(server, &videoService{session: session})

	log.Println("gRPC server listening on :50051")
	if err := server.Serve(listener); err != nil {
		log.Fatalf("Failed to serve gRPC: %v", err)
	}
}
