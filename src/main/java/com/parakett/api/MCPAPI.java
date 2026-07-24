package com.parakett.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.mcp.MCPRegistry;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/fs/api/v1/mcp")
public class MCPAPI {

	@GetMapping(produces = "application/json", path = "/servers")
	public ResponseEntity<Object> getMCPServersList(HttpServletRequest request) {

		ArrayNode servers = MCPRegistry.getServersList();

		ObjectNode result = JsonUtils.MAPPER.createObjectNode();
		result.put("status", "success");
		result.set("mcpServers", servers);
		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
				.body(result.toString());

	}

	@GetMapping(produces = "application/json", path = "/servers/{serverName}/describe")
	public ResponseEntity<Object> describeServer(HttpServletRequest request,
			@PathVariable("serverName") String serverName) {

		ArrayNode tools = MCPRegistry.getServer(serverName).getToolListJSON();
		ObjectNode result = JsonUtils.MAPPER.createObjectNode();
		result.put("status", "success");
		result.set("tools", tools);
		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
				.body(result.toString());
	}

}