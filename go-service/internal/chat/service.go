package chat

import (
	"context"
	"fmt"
	"log"

	"github.com/gocql/gocql"
	"github.com/google/uuid"
)

type Service struct {
	session *gocql.Session
}

type Message struct {
	ID         string
	SenderID   string
	ReceiverID string
	Content    string
	Timestamp  int64
	IsRead     bool
}

func NewService(session *gocql.Session) *Service {
	return &Service{session: session}
}

func (s *Service) SendMessage(ctx context.Context, senderID, receiverID, content string) (string, error) {
	messageID := uuid.New()
	timestamp := int64(1705688444)

	senderUUID, err := gocql.ParseUUID(senderID)
	if err != nil {
		return "", fmt.Errorf("invalid sender ID: %w", err)
	}

	receiverUUID, err := gocql.ParseUUID(receiverID)
	if err != nil {
		return "", fmt.Errorf("invalid receiver ID: %w", err)
	}

	err = s.session.Query(`
		INSERT INTO chat_keyspace.messages (id, sender_id, receiver_id, content, timestamp, is_read)
		VALUES (?, ?, ?, ?, ?, ?)
	`,
		messageID,
		senderUUID,
		receiverUUID,
		content,
		timestamp,
		false,
	).Exec()

	if err != nil {
		return "", fmt.Errorf("failed to insert message: %w", err)
	}

	log.Printf("Message sent from %s to %s", senderID, receiverID)
	return messageID.String(), nil
}

func (s *Service) GetChatHistory(ctx context.Context, userID, peerID string, limit, offset int) ([]Message, error) {
	var messages []Message

	userUUID, err := gocql.ParseUUID(userID)
	if err != nil {
		return nil, fmt.Errorf("invalid user ID: %w", err)
	}

	peerUUID, err := gocql.ParseUUID(peerID)
	if err != nil {
		return nil, fmt.Errorf("invalid peer ID: %w", err)
	}

	iter := s.session.Query(`
		SELECT id, sender_id, receiver_id, content, timestamp, is_read
		FROM chat_keyspace.messages
		WHERE (sender_id = ? AND receiver_id = ?) 
		   OR (sender_id = ? AND receiver_id = ?)
		LIMIT ?
	`,
		userUUID,
		peerUUID,
		peerUUID,
		userUUID,
		limit,
	).Iter()

	var id, senderID, receiverID gocql.UUID
	var content string
	var timestamp int64
	var isRead bool

	for iter.Scan(&id, &senderID, &receiverID, &content, &timestamp, &isRead) {
		messages = append(messages, Message{
			ID:         id.String(),
			SenderID:   senderID.String(),
			ReceiverID: receiverID.String(),
			Content:    content,
			Timestamp:  timestamp,
			IsRead:     isRead,
		})
	}

	if err := iter.Close(); err != nil {
		return nil, fmt.Errorf("failed to read chat history: %w", err)
	}

	return messages, nil
}
