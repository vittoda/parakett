package com.parakett.api;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.api.objects.UnholdRequest;
import com.parakett.flow.FlowRunLog;
import com.parakett.flow.FlowRunner;
import com.parakett.flow.FlowRunnerSessions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/fs/api/v1/sessions")
public class SessionsAPI {

    @GetMapping(produces = "application/json", path = "")
    public ResponseEntity<Object> getSessionsList(HttpServletRequest request) {

        ArrayNode sessions = FlowRunnerSessions.getAll();

        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status", "success");
        result.set("sessions", sessions);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());

    }

    @GetMapping(produces = "application/json", path = "/{sessionId}/items")
    public ResponseEntity<Object> getSesisonStepList(HttpServletRequest request,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @PathVariable("sessionId") String sessionId) {

        FlowRunner runner = FlowRunnerSessions.getBySessionId(sessionId);
        if (runner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Invalid session Id"));
        }
        FlowRunLog log = runner.getRunLog();
        if (log == null) {
            ObjectNode result = JsonUtils.MAPPER.createObjectNode();
            result.put("status", "success");
            result.put("logDisabled", true);
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .body(result.toString());
        }
        ArrayNode list = log.getLogList(offset, 100);

        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status", "success");
        result.set("items", list);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());

    }

    @GetMapping(produces = "application/json", path = "/{sessionId}/items/{itemIndex}")
    public ResponseEntity<Object> getSessionStepDetails(HttpServletRequest request,
            @PathVariable("sessionId") String sessionId, @PathVariable("itemIndex") Integer itemIndex) {

        FlowRunner runner = FlowRunnerSessions.getBySessionId(sessionId);
        if (runner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Invalid session Id"));
        }
        FlowRunLog log = runner.getRunLog();
        ObjectNode itemDetails = log.getExtendedDetailsForItemAtIndex(itemIndex);
        if (itemDetails == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Invalid item index"));
        }

        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status", "success");
        result.set("details", itemDetails);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());

    }

    @PostMapping(produces = "application/json", consumes = "application/json", path = "/{sessionId}/debug/next")
    public ResponseEntity<Object> debugNextStep(HttpServletRequest request,
            @PathVariable("sessionId") String sessionId) {

        FlowRunner runner = FlowRunnerSessions.getBySessionId(sessionId);
        if (runner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Invalid session Id"));
        }
        runner.debugNextStep();

        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status", "success");
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());

    }

    @PostMapping(produces = "application/json", consumes = "application/json", path = "/{sessionId}/abort")
    public ResponseEntity<Object> abort(HttpServletRequest request,
            @PathVariable("sessionId") String sessionId) {

        FlowRunner runner = FlowRunnerSessions.getBySessionId(sessionId);
        if (runner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Invalid session Id"));
        }
        runner.abort();

        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status", "success");
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());
    }

    @PostMapping(produces = "application/json", consumes = "application/json", path = "/{sessionId}/approve")
    public ResponseEntity<Object> approveSession(HttpServletRequest request,
            @Valid @RequestBody UnholdRequest r,
            @PathVariable("sessionId") String sessionId) {

        FlowRunner runner = FlowRunnerSessions.getBySessionId(sessionId);
        if (runner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Invalid session Id"));
        }

        ObjectNode response = JsonUtils.MAPPER.createObjectNode();
        response.put("status", r.getStatus());
        response.put("result", r.getResponse());
        runner.unholdExecution(FlowRunner.UnholdMode.NORMAL, response);

        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status", "success");
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());
    }

    @PostMapping(produces = "application/json", consumes = "application/json", path = "/{sessionId}/debug/abort")
    public ResponseEntity<Object> abortDebug(HttpServletRequest request,
            @PathVariable("sessionId") String sessionId) {

        FlowRunner runner = FlowRunnerSessions.getBySessionId(sessionId);
        if (runner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Invalid session Id"));
        }
        runner.abortDebug();

        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status", "success");
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());

    }
}
