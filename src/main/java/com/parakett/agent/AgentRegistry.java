package com.parakett.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AgentRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRegistry.class);

    private static HashMap<String, AgentCard> _mAgents = new HashMap<>();

    public static void add(Agent agent) {
        _mAgents.put(agent.id, new AgentCard(agent.id, agent, false));
    }

    public static Agent getById(String id) {
        return _mAgents.get(id).getAgentInstance();
    }

    public static boolean exists(String agentId) {
        return _mAgents.containsKey(agentId);
    }

    public static ArrayNode getAgentList() {
        ArrayNode nodes = JsonUtils.MAPPER.createArrayNode();
        for (AgentCard agent : _mAgents.values()) {
            nodes.add(agent.getAgentInstance().getJSON(false));
        }

        return nodes;
    }

    public static ArrayNode getAgentListForDiscovery() {
        ArrayNode nodes = JsonUtils.MAPPER.createArrayNode();
        for (AgentCard agent : _mAgents.values()) {
            nodes.add(agent.getAgentInfo(false));
        }

        return nodes;
    }

    public static void saveAgentDef(Agent agent) throws IOException {
        // Load existing content.
        String fileName = "agentOverrides.json";
        ObjectNode originalList = null;

        if (new File(fileName).exists()) {
            try (FileInputStream fis = new FileInputStream(new File(fileName))) {
                originalList = (ObjectNode) JsonUtils.MAPPER.readTree(fis);
            }
        } else {
            originalList = JsonUtils.MAPPER.createObjectNode();
        }
        originalList.set(agent.id, agent.getDefToSave());
        try (FileOutputStream fos = new FileOutputStream(new File(fileName))) {
            fos.write(originalList.toPrettyString().getBytes());
        }
    }

    public static void loadFromFile(String file) throws IOException {
        String overridesFile = "agentOverrides.json";
        JsonNode overrides = null;
        if (new File(overridesFile).exists()) {
            try (FileInputStream is = new FileInputStream(new File(overridesFile))) {
                overrides =  JsonUtils.MAPPER.readTree(is);
            }
        }
        try (FileInputStream is = new FileInputStream(new File(file))) {
            JsonNode on = JsonUtils.MAPPER.readTree(is);
            ArrayNode agentList = (ArrayNode) on.get("agents");
            int size = agentList.size();
            for (int i = 0; i < size; i++) {
                JsonNode agentNode = agentList.get(i);
                Agent agentInstance = new Agent();
                try {
                    agentInstance.loadAgentFromJSON(agentNode, file);
                } catch (IOException e) {
                    //For some reason, this agent could not be loaded. Abort and proceed with next
                    LOGGER.error("Error loading agent",e);
                    continue;
                }
                if (overrides != null && overrides.has(agentInstance.id)) {
                    agentInstance.loadFromOverrides( overrides.get(agentInstance.id));
                }

                AgentRegistry.add(agentInstance);
                agentInstance.initialize();
            }
        }

        for (AgentCard agent : _mAgents.values()) {
            List<String> agentIds = agent.getAgentInstance().getAvailableAgentIds();
            for (String id : agentIds) {
                // Do not allow recursive names.
                if (id.equals(agent.getId())) {
                    continue;
                }

                AgentCard requiredAgent = _mAgents.get(id);
                agent.getAgentInstance().setAvailableAgentFor(id, requiredAgent);
            }
            agent.getAgentInstance().updateAgentCapabilities();
        }

    }

}
