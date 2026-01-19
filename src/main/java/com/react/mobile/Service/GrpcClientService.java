package com.react.mobile.Service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Slf4j
@Service
public class GrpcClientService {

    @Value("${grpc.go-service.host:chat-video}")
    private String goServiceHost;

    @Value("${grpc.go-service.port:50051}")
    private int goServicePort;

    private ManagedChannel channel;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder
                .forAddress(goServiceHost, goServicePort)
                .usePlaintext()
                .build();
        log.info("gRPC channel initialized: {}:{}", goServiceHost, goServicePort);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            log.info("gRPC channel shutdown");
        }
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    // Add chat service client
    public void sendChatMessage(String senderId, String receiverId, String content) {
        try {
            log.info("Sending chat message from {} to {}", senderId, receiverId);
            // TODO: Implement gRPC call to Go service
            // ChatServiceGrpc.ChatServiceBlockingStub stub = ChatServiceGrpc.newBlockingStub(channel);
            // Message response = stub.sendMessage(Message.newBuilder()
            //     .setSenderId(senderId)
            //     .setReceiverId(receiverId)
            //     .setContent(content)
            //     .setTimestamp(System.currentTimeMillis())
            //     .build());
        } catch (Exception e) {
            log.error("Error sending chat message", e);
        }
    }

    // Add video service client
    public void initiateCall(String callerId, String calleeId, String callType) {
        try {
            log.info("Initiating {} call from {} to {}", callType, callerId, calleeId);
            // TODO: Implement gRPC call to Go service
            // VideoServiceGrpc.VideoServiceBlockingStub stub = VideoServiceGrpc.newBlockingStub(channel);
            // CallResponse response = stub.initiateCall(CallRequest.newBuilder()
            //     .setCallerId(callerId)
            //     .setCalleeId(calleeId)
            //     .setCallType(callType)
            //     .build());
        } catch (Exception e) {
            log.error("Error initiating call", e);
        }
    }
}
