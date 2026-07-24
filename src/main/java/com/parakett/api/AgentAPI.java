package com.parakett.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.agent.Agent;
import com.parakett.agent.AgentRegistry;
import com.parakett.api.objects.AgentConfig;
import com.parakett.api.objects.AgentRunRequest;
import com.parakett.api.objects.VariableRequestObject;
import com.parakett.flow.FlowRunner;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/fs/api/v1/agents")
public class AgentAPI {

	@GetMapping(produces = "application/json", path = "")
	public ResponseEntity<Object> getAgentList(HttpServletRequest request) {

		ObjectNode result = JsonUtils.MAPPER.createObjectNode();
		result.put("status", "success");
		result.set("agents", AgentRegistry.getAgentList());
		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
				.body(result.toString());

	}

	@PostMapping(consumes = "application/json", produces = "application/json", path = "/{agentId}")
	public ResponseEntity<Object> saveAgentDetails(HttpServletRequest request,
			@Valid @RequestBody AgentConfig agent,
			@PathVariable(name = "agentId") String agentId) {

		ObjectNode config = JsonUtils.MAPPER.createObjectNode();
		config.put("debug", agent.getEnableDebug());
		config.put("logging", agent.getEnableLog());
		config.put("archive", agent.getEnableArchive());
		Agent agentInstance = AgentRegistry.getById(agentId);
		agentInstance.updateConfig(config);
		try {
			AgentRegistry.saveAgentDef(agentInstance);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "error", "message", "Internal error saving agent details"));
		}
		ObjectNode result = JsonUtils.MAPPER.createObjectNode();
		result.put("status", "success");
		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
				.body(result.toString());

	}

	@PostMapping(consumes = "application/json", produces = "application/json", path = "/{agentId}/testrun")
	public ResponseEntity<Object> agentTestRun(HttpServletRequest request,
			@Valid @RequestBody AgentRunRequest runRequest,
			@PathVariable(name = "agentId") String agentId) {

		Agent agentInstance = AgentRegistry.getById(agentId);
		if(agentInstance == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("status", "error", "message", "Agent not found"));
		}

		try {

			HashMap<String, Object> variableValues = new HashMap<>();

			if (runRequest.getVariables() != null) {
				List<VariableRequestObject> vals = runRequest.getVariables();
				for (VariableRequestObject v : vals) {
					// runner.setVariable(v.getName(), v.getValue());
					variableValues.put(v.getName(), v.getValue());
				}
			}

			FlowRunner runner = agentInstance.run(runRequest.getInput(), null, variableValues,
				runRequest.getEnableDebug(), runRequest.getEnableLog(), runRequest.getEnableArchive(), null, null, null);

			ObjectNode result = JsonUtils.MAPPER.createObjectNode();
			result.put("status", "success");
			result.put("sessionId", runner.getSessionId());
			return ResponseEntity
					.ok()
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
					.body(result.toString());
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "error", "message", "Internal error starting flow"));
		}

	}

	@GetMapping(produces = "application/json", path = "/{agentId}")
	public ResponseEntity<Object> getAgentDetails(HttpServletRequest request,
			@PathVariable(name = "agentId") String agentId) {

		ObjectNode result = JsonUtils.MAPPER.createObjectNode();
		result.put("status", "success");
		result.set("agent", AgentRegistry.getById(agentId).getJSON(true));
		return ResponseEntity
				.ok()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
				.body(result.toString());

	}

}
