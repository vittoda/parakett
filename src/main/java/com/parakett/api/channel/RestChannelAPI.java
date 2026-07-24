package com.parakett.api.channel;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.channels.base.JsonUtils;
import com.parakett.channels.base.OutputMessage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/fs/api/v1/channels/rest")
public class RestChannelAPI {

    @PostMapping(produces = "application/json", consumes = "application/json", path = "/{agentId}/message")
    public ResponseEntity<Object> setDefaultModel(HttpServletRequest request,
            @PathVariable(name = "agentId") String agentId,
            @Valid @RequestBody RestChannelMessage restMessage) {

        //Get the client instance for this agent
        RestChannelInstance ri = (RestChannelInstance) RestChannel.INSTANCE.getInstance(agentId);
        OutputMessage  om = ri.onEventMessageReceived(restMessage.getMessage());

        ObjectNode result = JsonUtils.MAPPER.createObjectNode();
        result.put("status","success");
        result.put("result", om.getText());
       
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .body(result.toString());

    }
    
}
