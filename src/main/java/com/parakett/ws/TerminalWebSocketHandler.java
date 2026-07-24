package com.parakett.ws;

import java.util.LinkedList;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.cli.CliCommand;
import com.parakett.cli.CliCommandRegistry;
import com.parakett.flow.FlowRunner.UnholdMode;
import com.parakett.flow.FlowRunnerSessions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminalWebSocketHandler.class);

    // Holds the session connection info mapped by a unique User/Client ID

    private static final String USER_RESPONSE_COMMAND = "userResponse";

    private static final LinkedList<String> INTERNAL_COMMANDS = new LinkedList<>();

    static {
        INTERNAL_COMMANDS.add("set");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract a unique identifier for the client (e.g., from query params, headers,
        // or auth principal)
        String clientId = _getClientId(session);

        if (clientId != null) {
            ClientSession cl = new ClientSession(clientId, session);
            WSClientSessionCache.add(cl);
            LOGGER.info("Connection established with client '{}'" , clientId);
            ObjectNode result = JsonUtils.MAPPER.createObjectNode();
            result.put("status", "success");
            String version = TerminalWebSocketHandler.class.getPackage().getImplementationVersion();
            if (version == null) {
                version = "Unknon-version";
            }
            result.put("message", "Server Version : "+version);
            _sendMessageToClient(clientId, result.toString());
            return;
        } else {
            session.close(CloseStatus.BAD_DATA.withReason("Missing client identifier"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String clientId = _getClientId(session);
        String payload = message.getPayload();
        JsonNode node =  JsonUtils.MAPPER.readTree(payload);
        String commandName = node.get("command").asText();
        if(commandName.equals(USER_RESPONSE_COMMAND)) {
            handleUserRespose((ObjectNode)node.get("params"));
            return;
        }
        if (INTERNAL_COMMANDS.contains(commandName)) {
            if (commandName.equals("set")) {
                String params = null;
                if (!node.get("params").isNull()) {
                    if (node.get("params").asText().trim().length() != 0) {
                        params = node.get("params").asText().trim();
                    }
                }

                if (params == null) {
                    // We are responding with all values
                    ObjectNode result = JsonUtils.MAPPER.createObjectNode();
                    result.put("status", "success");
                    result.put("type", "key-value");
                    ClientSession cl = WSClientSessionCache.get(clientId);
                    result.set("message", cl.getAllVars());
                    _sendMessageToClient(clientId, result.toString());
                    return;
                }

                String[] items = params.split("=");
                if (items.length == 2) {
                    // We are setting
                    ClientSession cl = WSClientSessionCache.get(clientId);
                    cl.setVar(items[0], items[1]);
                    ObjectNode result = JsonUtils.MAPPER.createObjectNode();
                    result.put("status", "success");
                    result.put("type", "done");
                    _sendMessageToClient(clientId, result.toString());

                } else if (items.length == 1) {
                    // We are setting
                    ClientSession cl = WSClientSessionCache.get(clientId);
                    cl.clearVar(items[0]);
                    ObjectNode result = JsonUtils.MAPPER.createObjectNode();
                    result.put("status", "success");
                    result.put("type", "done");
                    _sendMessageToClient(clientId, result.toString());

                } else {
                    ObjectNode result = JsonUtils.MAPPER.createObjectNode();
                    result.put("status", "error");
                    result.put("message",
                            "Command 'set' needs at least one and maximum two parameters. A key and a value");
                    _sendMessageToClient(clientId, result.toString());
                }
                return;

            }
        }
        CliCommand command = CliCommandRegistry.getCommand(commandName);
        if (command == null) {
            ObjectNode result = JsonUtils.MAPPER.createObjectNode();
            result.put("status", "error");
            result.put("message", "Unsupported command '" + commandName + "'");
            _sendMessageToClient(clientId, result.toString());
            return;
        }

        // Capability: Server can send a message back as an immediate response
        try {
            String params = node.get("params").asText();
            _sendMessageToClient(clientId,
                    command.run(params, WSClientSessionCache.get(clientId)).responseObject.toString());
        } catch (Exception e) {
            ObjectNode result = JsonUtils.MAPPER.createObjectNode();
            result.put("status", "error");
            result.put("message", e.getMessage());
            _sendMessageToClient(clientId, result.toString());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String clientId = _getClientId(session);
        if (clientId != null) {
            WSClientSessionCache.remove(clientId);
            LOGGER.info("Connection closed for client '{}", clientId);
        }
    }

    // Capability: Server can send unsolicited events/messages to a specific client
    private void _sendMessageToClient(String clientId, String message) {
        WSClientSessionCache.get(clientId).sendMessageToClient(message);
    }

    // Helper to extract client ID from the connection URL query parameters (e.g.,
    // ?clientId=user123)
    private String _getClientId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("clientId=")) {
            return query.split("clientId=")[1].split("&")[0];
        }
        // Fallback to Session ID if no custom identifier is passed
        return session.getId();
    }

    private void handleUserRespose(ObjectNode userResponse) {
        if (userResponse != null && !userResponse.isMissingNode()) {
            // This is a JSON. Try to understand
            String sessionId = userResponse.get("requestId").asText(); // RequestId will be session Id
            userResponse.remove("requestId");
            FlowRunnerSessions.getBySessionId(sessionId).unholdExecution(UnholdMode.NORMAL,
                    userResponse);
        }
    }

}
