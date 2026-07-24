package com.parakett.flow;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.parakett.agent.VariableDef;
import com.parakett.mcp.MCPRegistry;

public class PromptGenerator {

    private static String _mSystemPrrompt = null;
    private static String _mDomainCommonMessage = null;

    public List<String> getInitialStepSystemInstructionsForRoot(String domainContextContent, List<String> toolNames,
            ArrayNode availableAgents,
            HashMap<String, VariableDef> variables) {
        try {

            if (_mSystemPrrompt == null) {
                InputStream is = AgentTool.class
                        .getClassLoader()
                        .getResourceAsStream("systemPrompt.txt");

                _mSystemPrrompt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                is.close();
            }

            LinkedList<String> domainContext = getInitialStepSystemInstructionsForDomain(domainContextContent,
                    toolNames, availableAgents, variables);

            // Add system context
            domainContext.add(0, _mSystemPrrompt);

            return domainContext;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public LinkedList<String> getInitialStepSystemInstructionsForDomain(String domainContext, List<String> toolNames,
            ArrayNode agentsAndCapabilities,
            HashMap<String, VariableDef> variables) {

        try {
            if (_mDomainCommonMessage == null) {
                InputStream is = AgentTool.class
                        .getClassLoader()
                        .getResourceAsStream("domainCommonMessages.txt");

                _mDomainCommonMessage = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                is.close();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinkedList<String> systemInstructions = new LinkedList<>();
        systemInstructions.add(domainContext);

        // Tools
        if (toolNames.size() > 0) {
            toolNames.sort(String::compareTo);
            StringBuilder tools = new StringBuilder("#TOOLS\n(Format - Tool name:Description)\n\n");
            for (String toolName : toolNames) {
                JsonNode desc = MCPRegistry.getToolDefinitionForToolCalling(toolName);
                tools.append(desc.get("name").asText()).append(":").append(desc.get("description").asText())
                        .append("\n");
            }

            tools.append("##IMPORTANT:\n1. Do not use any tools other the ones listed above for steps\n\n");
            systemInstructions.add(tools.toString());
        }
        else {

        }

        // Add any variables.
        StringBuilder vars = new StringBuilder("#VARIABLES\n(Format - Variable name:Description)\n\n");
        if (variables.size() > 0) {
            for (String k : variables.keySet()) {
                vars.append(k).append(":").append(variables.get(k).description).append("\n");
            }

            systemInstructions.add(vars.toString());
        }
        else {
            systemInstructions.add("None. There are absolutely no runtime variables defined for this execution. Do not use flower braces {{}} anywhere in your output.\n");
        }

        if (agentsAndCapabilities.size() > 0) {
            StringBuilder availableAgents = new StringBuilder("#Agents and capabilities\n\n")
                    .append(agentsAndCapabilities.toString());
            systemInstructions.add(availableAgents.toString());
        }

        // Add domain common instructions.
        systemInstructions.add(_mDomainCommonMessage);
        return systemInstructions;

    }

}
