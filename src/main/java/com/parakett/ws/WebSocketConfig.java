package com.parakett.ws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // Activates WebSocket infrastructure
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(terminalWebSocketHandler(), "/fs/api/v1/ws/message")
                .setAllowedOrigins("*"); // Adjust origins for production security
    }

    @Bean
    public TerminalWebSocketHandler terminalWebSocketHandler() {
        return new TerminalWebSocketHandler();
    }
}