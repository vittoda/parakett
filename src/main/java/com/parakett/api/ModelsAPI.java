package com.parakett.api;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.api.objects.DefaultModelRequest;
import com.parakett.models.ModelConnectionRegistry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/fs/api/v1/models")
public class ModelsAPI {

    @GetMapping(produces = "application/json", path = "")
    public ResponseEntity<Object> getModelList(HttpServletRequest request) {

        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status", "success");
        ArrayNode modelNames = ModelConnectionRegistry.getModelNameList();
        ArrayNode ret = JsonUtils.MAPPER.createArrayNode();
        String defaultModel = ModelConnectionRegistry.getDefaultModelName();
        for (int i = 0; i < modelNames.size(); i++) {
            String modelName = modelNames.get(i).asText();
            ObjectNode model = JsonUtils.MAPPER.createObjectNode();
            model.put("name", modelName);
            model.put("isDefault", defaultModel.equals(modelName));
            ret.add(model);
        }
        result.set("models", ret);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());

    }

    @PostMapping(produces = "application/json", consumes = "application/json", path = "/default")
    public ResponseEntity<Object> setDefaultModel(HttpServletRequest request,
            @Valid @RequestBody DefaultModelRequest defaultModel) {

        String modelName = defaultModel.getModelName();
        if(ModelConnectionRegistry.getConnectionForModel(modelName) == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("status", "error", "message", "Model not found"));
        }
        ModelConnectionRegistry.setDefaultModel(modelName);
        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status", "success");

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());

    }

}
