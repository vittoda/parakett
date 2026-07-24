package com.parakett.api;

import java.util.Date;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.metrics.MetricsDB;
import com.parakett.models.ModelConnectionRegistry;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/fs/api/v1/metrics")
public class MetricsAPI {

    @GetMapping(produces = "application/json", path = "/model/tokens/total")
    public ResponseEntity<Object> getTotalTokenForModel(HttpServletRequest request,
            @RequestParam(name = "modelName") String modelName) {

        try {
            ObjectNode result = JsonUtils.MAPPER.createObjectNode();
            result.put("status", "success");
            result.set("tokens", MetricsDB.getTotalMetricForModel(modelName));
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .body(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Internal error getting model metrics"));
        }
    }

    @GetMapping(produces = "application/json", path = "/model/tokens/today")
    public ResponseEntity<Object> getTokensTodayForModel(HttpServletRequest request,
            @RequestParam(name = "modelName") String modelName) {

        try {
            ObjectNode result = JsonUtils.MAPPER.createObjectNode();
            result.put("status", "success");
            result.set("tokens", MetricsDB.getTotalMetricForModelForDate(modelName, new Date()));
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .body(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Internal error getting model metrics"));
        }
    }

    @GetMapping(produces = "application/json", path = "/model/tokens/sinceuptime")
    public ResponseEntity<Object> getTokensSinceUptimeForModel(HttpServletRequest request,
            @RequestParam(name = "modelName") String modelName) {

        try {
            ObjectNode result = JsonUtils.MAPPER.createObjectNode();
            result.put("status", "success");
            result.set("tokens", MetricsDB.getTotalMetricForModelFrom(modelName, ModelConnectionRegistry.STARTUP_TIME));
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .body(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Internal error getting model metrics"));
        }
    }

    @GetMapping(produces = "application/json", path = "/model/requests/total")
    public ResponseEntity<Object> getRequestMetrics(HttpServletRequest request,
            @RequestParam(name = "modelName") String modelName) {

        try {
            ObjectNode result = JsonUtils.MAPPER.createObjectNode();
            result.put("status", "success");
            result.set("requests", MetricsDB.getTotalRequestMetricForModel(modelName));
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .body(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Internal error getting model request metrics"));
        }
    }

    @GetMapping(produces = "application/json", path = "/model/requests/today")
    public ResponseEntity<Object> getRequestMetricsForToday(HttpServletRequest request,
            @RequestParam(name = "modelName") String modelName) {
        try {
            ObjectNode result = JsonUtils.MAPPER.createObjectNode();
            result.put("status", "success");
            result.set("requests", MetricsDB.getRequestMetricForModelForToday(modelName, new Date()));
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .body(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Internal error getting model request metrics"));
        }
    }
}
