package com.parakett.api;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.agent.Agent;
import com.parakett.agent.AgentRegistry;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/fs/api/v1/agents/discovery")
public class AgentsDiscoveryAPI {

    @GetMapping(produces = "application/json", path = "/list")
    public ResponseEntity<Object> getListOfAgents(HttpServletRequest request) {
        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status", "success");
        result.set("agents", AgentRegistry.getAgentListForDiscovery());
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());
    }

    @GetMapping(produces = "application/json", path = "/{agentId}")
    public ResponseEntity<Object> discoverAgent(HttpServletRequest request,
            @PathVariable(name = "agentId") String agentId) {

        Agent agent = AgentRegistry.getById(agentId);
        if (agent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Invalid agent Id"));
        }

        ObjectNode result = JsonUtils.MAPPER.createObjectNode();

        result.put("status", "success");
        result.set("agent", agent.getAgentDiscoveryJSON(true));
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());

    }

}
