package com.react.mobile.Config;

import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Repository.AuthUserRepository;
import com.react.mobile.Service.CustomUserDetailsService;
import com.react.mobile.Service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthUserRepository authUserRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String username = jwtService.extractUsername(token);
            var userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.isTokenValid(token, userDetails)) {
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            AuthUser authUser = authUserRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            attributes.put("userId", authUser.getId());
            attributes.put("username", authUser.getUsername());
            return true;
        } catch (Exception ex) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }

    private String resolveToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.getFirst();
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }

        URI uri = request.getURI();
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            return null;
        }

        return List.of(query.split("&")).stream()
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2 && "token".equals(parts[0]))
                .map(parts -> parts[1])
                .findFirst()
                .map(this::decodeQueryValue)
                .orElse(null);
    }

    private String decodeQueryValue(String raw) {
        return Optional.ofNullable(raw)
                .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                .orElse(null);
    }
}
