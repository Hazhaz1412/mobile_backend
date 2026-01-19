package video

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

type Call struct {
	CallID    string
	CallerID  string
	CalleeID  string
	CallType  string // "audio" or "video"
	Status    string // "pending", "active", "ended"
	StartTime int64
	EndTime   int64
	Duration  int64
}

func NewService(session *gocql.Session) *Service {
	return &Service{session: session}
}

func (s *Service) InitiateCall(ctx context.Context, callerID, calleeID, callType string) (string, error) {
	callID := uuid.New().String()
	startTime := int64(1705688444)

	callIDUUID, err := uuid.Parse(callID)
	if err != nil {
		return "", fmt.Errorf("failed to parse call ID: %w", err)
	}

	callerIDUUID, err := uuid.Parse(callerID)
	if err != nil {
		return "", fmt.Errorf("failed to parse caller ID: %w", err)
	}

	calleeIDUUID, err := uuid.Parse(calleeID)
	if err != nil {
		return "", fmt.Errorf("failed to parse callee ID: %w", err)
	}

	err = s.session.Query(`
		INSERT INTO chat_keyspace.calls (call_id, caller_id, callee_id, call_type, status, start_time)
		VALUES (?, ?, ?, ?, ?, ?)
	`,
		callIDUUID,
		callerIDUUID,
		calleeIDUUID,
		callType,
		"pending",
		startTime,
	).Exec()

	if err != nil {
		return "", fmt.Errorf("failed to initiate call: %w", err)
	}

	log.Printf("Call initiated from %s to %s (type: %s)", callerID, calleeID, callType)
	return callID, nil
}

func (s *Service) EndCall(ctx context.Context, callID string, duration int64) error {
	endTime := int64(1705688444)

	callIDUUID, err := uuid.Parse(callID)
	if err != nil {
		return fmt.Errorf("failed to parse call ID: %w", err)
	}

	err = s.session.Query(`
		UPDATE chat_keyspace.calls
		SET status = ?, end_time = ?, duration = ?
		WHERE call_id = ?
	`,
		"ended",
		endTime,
		duration,
		callIDUUID,
	).Exec()

	if err != nil {
		return fmt.Errorf("failed to end call: %w", err)
	}

	log.Printf("Call %s ended. Duration: %d seconds", callID, duration)
	return nil
}

func (s *Service) GetActiveStreams(ctx context.Context, userID string) ([]Call, error) {
	var calls []Call

	userIDUUID, err := uuid.Parse(userID)
	if err != nil {
		return nil, fmt.Errorf("failed to parse user ID: %w", err)
	}

	iter := s.session.Query(`
		SELECT call_id, caller_id, callee_id, call_type, status, start_time, end_time, duration
		FROM chat_keyspace.calls
		WHERE (caller_id = ? OR callee_id = ?) AND status = ?
	`,
		userIDUUID,
		userIDUUID,
		"active",
	).Iter()

	var callID, callerID, calleeID gocql.UUID
	var callType, status string
	var startTime, endTime, duration int64

	for iter.Scan(&callID, &callerID, &calleeID, &callType, &status, &startTime, &endTime, &duration) {
		calls = append(calls, Call{
			CallID:    callID.String(),
			CallerID:  callerID.String(),
			CalleeID:  calleeID.String(),
			CallType:  callType,
			Status:    status,
			StartTime: startTime,
			EndTime:   endTime,
			Duration:  duration,
		})
	}

	if err := iter.Close(); err != nil {
		return nil, fmt.Errorf("failed to get active streams: %w", err)
	}

	return calls, nil
}
