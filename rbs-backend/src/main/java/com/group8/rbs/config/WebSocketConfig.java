package com.group8.rbs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // where the frontend subscribes
        config.setApplicationDestinationPrefixes("/app"); // where the frontend sends
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // <-- Pure WebSocket endpoint
                .setAllowedOriginPatterns("*");
    
        registry.addEndpoint("/ws-sockjs") // <-- SockJS fallback endpoint
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    
}
