package com.parakett.agent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.api.channel.RestChannel;
import com.parakett.channels.ChannelRegistry;
import com.parakett.channels.base.CommChannelBase;
import com.parakett.channels.base.CommChannelException;
import com.parakett.channels.base.CommChannelInstance;
import com.parakett.channels.base.InputMessage;
import com.parakett.channels.base.MessageContext;
import com.parakett.channels.base.OnMessageHandler;
import com.parakett.channels.base.OutputMessage;
import com.parakett.cli.CliChannel;
import com.parakett.flow.AgentTool;
import com.parakett.flow.FlowException;
import com.parakett.flow.FlowExecutions;
import com.parakett.flow.FlowMemory;
import com.parakett.flow.FlowRunner;
import com.parakett.flow.FlowRunner.UnholdMode;
import com.parakett.flow.FlowRunnerSessions;
import com.parakett.flow.PromptGenerator;
import com.parakett.flow.Step;
import com.parakett.flow.StepGroup;
import com.parakett.mcp.MCPRegistry;
import com.parakett.mcp.MCPServer;
import com.parakett.mcp.MCPServerInstance;
import com.parakett.models.ModelConnectionRegistry;
import com.parakett.models.ModelSystemMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Agent {

    private static final String AGENT_BASE_URL = "http://localhost:8080/fs/api/v1/agent/a2a";

    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

    // Configs
    private JsonNode _mSystemDef = null;
    private JsonNode _mOverrideDef = null;
    private HITLConfig _mHITLConfig = null;

    public String name = null;
    public boolean hasErrors = false;
    public String id = null;
    public String description = null;

    private String _mContext = null;

    private List<String> _mMCPServerNames = new LinkedList<>();
    private List<String> _mAgentIds = new LinkedList<>();

    // Agent holding agent card, recursive dependency. Not a good design. But who
    // cares. It works.
    private HashMap<String, AgentCard> _mAvailableAgentCards = new HashMap<>();
    private List<String> _mToolNames = null;

    public boolean debug = false;
    public boolean logging = false;
    public boolean archive = false;

    private HashMap<String, VariableDef> _mVariableDefs = new HashMap<>();
    private HashMap<String, MCPServerInstance> _mMCPServers = new HashMap<>();
    private HashMap<String, AgentCommChannel> _mChannels = new HashMap<>();

    private ArrayNode _mAgentCapabilities = null;

    private String _mModel = null;

    Agent() {

    }

    public Agent(String id, String name, String model, String description) {
        this.id = id;
        this.name = name;
        this._mModel = model;
        this.description = description;
    }

    JsonNode getDefToSave() {
        return _mOverrideDef;
    }

    public JsonNode getSystemDef() {
        return _mSystemDef;
    }

    public HITLConfig getHITLConfig() {
        return _mHITLConfig;
    }

    public String getModelName() {
        return _mModel;
    }

    public void updateConfig(JsonNode node) {
        if (!node.at("/debug").isMissingNode()) {
            this.debug = node.at("/debug").booleanValue();
        }
        if (!node.at("/logging").isMissingNode()) {
            this.logging = node.at("/logging").booleanValue();
        }
        if (!node.at("/archive").isMissingNode()) {
            this.archive = node.at("/archive").booleanValue();
        }

        _mOverrideDef = node.deepCopy();
    }

    // Start orchestration poller.
    void startPoller() {
        Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    AgentMessage am = Orchestrator.INSTANCE.poll(this.id);
                    try {
                        MessageContext ctx = am.triggerringFlow != null
                                ? am.triggerringFlow.getTriggerringMessageContext()
                                : null;
                        CommChannelInstance channel = am.triggerringFlow != null
                                ? am.triggerringFlow.getTriggerringChannel()
                                : null;
                        this.run(am.prompt, am.additionalContext,
                                ctx, channel, am.triggerringFlow);
                    } catch (FlowException e) {
                        e.printStackTrace();
                    }
                } catch (OrchestratorException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
    }

    public ObjectNode getJSON(boolean extended) {

        ObjectNode node = JsonUtils.MAPPER.createObjectNode();
        node.put("name", this.name);

        node.put("id", this.id);
        node.put("description", this.description);
        node.put("hasErrors", this.hasErrors);
        node.put("model", this._mModel);
        if (extended) {
            node.put("context", this._mContext);
            node.put("debug", this.debug);
            node.put("logging", this.logging);
            node.put("archive", this.archive);

            ArrayNode channels = JsonUtils.MAPPER.createArrayNode();
            for (String key : _mChannels.keySet()) {
                channels.add(_mChannels.get(key).getJSON());
            }
            node.set("channels", channels);

            if (_mVariableDefs.size() > 0) {
                ArrayNode variables = JsonUtils.MAPPER.createArrayNode();
                for (String k : _mVariableDefs.keySet()) {
                    ObjectNode n = JsonUtils.MAPPER.createObjectNode();
                    VariableDef def = _mVariableDefs.get(k);
                    n.put("name", def.name);
                    n.put("label", def.label);
                    variables.add(n);
                }
                node.set("variables", variables);
            }

            ArrayNode mcpServers = JsonUtils.MAPPER.createArrayNode();
            for (String name : _mMCPServers.keySet()) {
                ObjectNode n = JsonUtils.MAPPER.createObjectNode();
                n.put("name", name);
                MCPServerInstance mi = _mMCPServers.get(name);
                n.put("category", mi.getServerDef().category);
                mcpServers.add(n);
            }
            node.set("mcpServers", mcpServers);
        }

        return node;
    }

    void loadAgentFromJSON(JsonNode an, String file) throws IOException {
        this.name = an.get("name").asText();
        this.id = an.get("id").asText();
        this.description = an.get("description").asText();
        ArrayNode serversList = (ArrayNode) an.get("mcpServers");
        this._mMCPServerNames = new LinkedList<>();
        for (int i = 0; i < serversList.size(); i++) {
            _mMCPServerNames.add(serversList.get(i).asText());
        }

        JsonNode node = an.get("context");
        if (node.isTextual()) {
            this._mContext = node.asText();
        } else {
            String contextFile = node.get("file").asText();
            if ((!new File(contextFile).exists())) {
                Path path = Paths.get(file);
                Path parent = path.getParent();
                if (parent != null) {
                    contextFile = parent + File.separator + contextFile;
                }
            }
            this._mContext = Files.readString(Paths.get(contextFile), StandardCharsets.UTF_8);

        }
        _mSystemDef = an;
        if (an.has("hitlConfig")) {
            this._mHITLConfig = HITLConfig.loadFromJSON(an.get("hitlConfig"));
        }

        if (an.has("variables")) {
            ArrayNode variables = (ArrayNode) an.get("variables");
            for (int i = 0; i < variables.size(); i++) {
                JsonNode on = variables.get(i);
                VariableDef vdef = new VariableDef(on.get("name").asText(), on.get("label").asText(),
                        on.get("description").asText());
                _mVariableDefs.put(vdef.name, vdef);
            }
        }

        if (an.has("model")) {
            String definedModel = an.get("model").asText();
            // Check if the model exists. If not, abort. We cannot add
            if (ModelConnectionRegistry.getConnectionForModel(definedModel) == null) {
                throw new IOException(
                        "Model '" + definedModel + "' defined for magent, does not exist. Agent cannot be created");
            }
            _mModel = definedModel;
        } else {
            _mModel = ModelConnectionRegistry.getDefaultModelName();
        }

        if (an.has("channels")) {
            ArrayNode channels = (ArrayNode) an.get("channels");
            for (int i = 0; i < channels.size(); i++) {
                String channelInstanceId = channels.get(i).asText();
                CommChannelInstance channelInstance = ChannelRegistry.createInstanceFor(channelInstanceId, this.id);
                if (channelInstance != null) {
                    CommChannelBase channel = ChannelRegistry.getChannelFor(channelInstanceId);
                    try {
                        channelInstance.initialize();
                        channelInstance.registerOnMessageHandler(new OnMessageHandler() {
                            @Override
                            public OutputMessage onMessageReceived(InputMessage msg) {
                                // Check if we have received, a response for HIL
                                String m = msg.getText().stripLeading();
                                if (m.startsWith("{")) {
                                    try {
                                        JsonNode responseObject = JsonUtils.MAPPER.readTree(m);
                                        if (responseObject.has("type")) {
                                            String type = responseObject.get("type").asText();
                                            if (type.equals("hilConfirmationResponse")) {
                                                handleUserConfirmationResponse(
                                                        (ObjectNode) responseObject.get("content"));
                                                return null;
                                            }
                                        }

                                    } catch (Exception e) {
                                        /*
                                         * TODO: As of now if there is any exception, we are defaulting to trigger new
                                         * agent flow.
                                         * This is a big problem, as it will trigger new agent, even if it is mean to
                                         * unhold a flow.
                                         */
                                        LOGGER.error(
                                                "Response recieved from channel has parsing errors as JSON. Skipping rest of the processing",
                                                e);
                                    }
                                }
                                String userMessage = "Message Source : communication channel event\n\n" + m;
                                try {
                                    run(userMessage, null, null, debug, logging, archive,
                                            msg.getContext(), channelInstance, null);
                                    return null;
                                } catch (FlowException e) {
                                    // TODO:Handle this
                                    e.printStackTrace();
                                    return null;
                                }
                            }
                        });

                        _mChannels.put(name,
                                new AgentCommChannel(channelInstanceId, channel.getKey(), channel.getName(), false));
                    } catch (CommChannelException e) {
                        e.printStackTrace();
                        hasErrors = true;
                        _mChannels.put(name,
                                new AgentCommChannel(channelInstanceId, channel.getKey(), channel.getName(), true));
                    } catch (Exception e) {
                        e.printStackTrace();
                        hasErrors = true;
                    }

                }
            }
        }

        // Add CLI channel
        CommChannelInstance cl = CliChannel.INSTANCE.createInstance(this.id, null);
        cl.registerOnMessageHandler(new AgentCliCommMessageHandler(this, cl));

        // Add REST channel
        CommChannelInstance ri = RestChannel.INSTANCE.createInstance(this.id, null);
        ri.registerOnMessageHandler(new AgentRestMessageHandler(this, cl));

        if (an.has("agents")) {
            // For now, just add the agent names. We will load agent definition later.
            ArrayNode agents = (ArrayNode) an.get("agents");
            _mAgentIds.clear();
            for (int i = 0; i < agents.size(); i++) {
                _mAgentIds.add(agents.get(i).asText());
            }
        }
    }

    private void handleUserConfirmationResponse(ObjectNode userResponse) {
        String sessionId = userResponse.get("requestId").asText(); // RequestId will be session Id
        userResponse.remove("requestId");
        FlowRunnerSessions.getBySessionId(sessionId).unholdExecution(UnholdMode.NORMAL,
                userResponse);
    }

    void loadFromOverrides(JsonNode node) {
        if (!node.at("/debug").isMissingNode()) {
            this.debug = node.at("/debug").booleanValue();
        }
        if (!node.at("/logging").isMissingNode()) {
            this.logging = node.at("/logging").booleanValue();
        }

        if (!node.at("/archive").isMissingNode()) {
            this.archive = node.at("/archive").booleanValue();
        }

        _mOverrideDef = node.deepCopy();
    }

    public void setContext(String context) {
        this._mContext = context;
    }

    public void initialize() {
        _mToolNames = new LinkedList<>();
        for (String serverName : _mMCPServerNames) {
            MCPServer server = MCPRegistry.getServer(serverName);
            if (server != null) {

                List<String> serverToolNames = server.getToolNames(true);
                _mToolNames.addAll(serverToolNames);
                if (server.hasErrors) {
                    this.hasErrors = true;
                    continue;
                }
                // Create server instance
                MCPServerInstance instance = server.createInstance();
                _mMCPServers.put(serverName, instance);
            } else {
                LOGGER.error("Agent initialization error. MCP server '{}'' not found", serverName);
            }

        }

        // Agent tools
        _mToolNames.addAll(AgentTool.getAllToolNames());

        // Register with orechstrator.
        Orchestrator.INSTANCE.registerAgent(this.id);
        startPoller();
    }

    void updateAgentCapabilities() {
        _mAgentCapabilities = JsonUtils.MAPPER.createArrayNode();
        for (String key : _mAvailableAgentCards.keySet()) {
            AgentCard ac = _mAvailableAgentCards.get(key);
            if (ac == null) {
                LOGGER.error("Agent instance '{}' not found. This is referred in '{}'", key, this.name);
                this.hasErrors = true;
                continue;
            }
            ObjectNode on = ac.getAgentInstance().getAgentDiscoveryJSON(true);
            on.remove("endpoint_url");
            _mAgentCapabilities.add(on);
        }
    }

    private FlowRunner prepareAndRunRootStep(String userMessage,
            List<String> additionalInstructions,
            HashMap<String, Object> variableValues,
            boolean debug, boolean logging, boolean archive,
            MessageContext triggerringMessageContext, CommChannelInstance triggerringChannel,
            FlowRunner triggerringFlow) throws FlowException {

        Step rootStep = new Step(Step.ROOT_STEP_NAME, "", userMessage, additionalInstructions, Step.STEP_TYPE_LLM,
                this._mModel, null,
                new String[0], new String[0], true, false, null);

        StepGroup sg = new StepGroup();
        sg.addStep(rootStep);
        String[] allSteps = { "root" };
        sg.addStep(Step.getErrorStrep(allSteps, _mModel));

        return runSteps(sg, Step.ROOT_STEP_NAME, variableValues, debug, logging, archive, triggerringMessageContext,
                triggerringChannel, triggerringFlow);
    }

    public FlowRunner run(String userMessage,
            List<String> additionalContext,
            MessageContext triggerringMessageContext,
            CommChannelInstance triggerringChannel,
            FlowRunner triggerringFlow) throws FlowException {
        boolean idebug = this.debug;
        boolean iarchive = this.archive;
        boolean ilogging = this.logging;

        if (triggerringFlow != null) {
            idebug = triggerringFlow.isDebugEnabled();
            iarchive = triggerringFlow.isArchiveEnabled();
            ilogging = triggerringFlow.isLogEnabled();
        }
        return run(userMessage, additionalContext, null, idebug, ilogging, iarchive,
                triggerringMessageContext, triggerringChannel, triggerringFlow);
    }

    public FlowRunner run(String userMessage,
            List<String> additionalContext,
            HashMap<String, Object> variableValues,
            boolean debug, boolean logging, boolean archive,
            MessageContext triggerringMessageContext, CommChannelInstance triggerringChannel,
            FlowRunner triggerringFlow) throws FlowException {

        // We are yet to generate the steps. So, use this to generate the steps and run.
        return prepareAndRunRootStep(userMessage, additionalContext, variableValues, debug, logging, archive,
                triggerringMessageContext,
                triggerringChannel, triggerringFlow);
    }

    FlowRunner runSteps(StepGroup sg, String startStep,
            HashMap<String, Object> variableValues,
            boolean debug, boolean logging, boolean archive,
            MessageContext triggerringMessageContext, CommChannelInstance triggerringChannel,
            FlowRunner triggerringFlow) {

        FlowRunner runner = FlowExecutions.getNewRunnerInstance(sg, debug, logging, archive,
                this);

        if (variableValues != null) {
            for (String key : variableValues.keySet()) {
                runner.setVariable(key, variableValues.get(key));
            }
        }
        if (triggerringMessageContext != null) {
            runner.setTriggerringMessageContext(triggerringMessageContext);
        }
        if (triggerringChannel != null) {
            runner.setTriggerringChannel(triggerringChannel);
        }
        if (triggerringFlow != null) {
            runner.setFlowToNotify(triggerringFlow.getSessionId());
        }

        // Clear the moemory
        FlowMemory memory = runner.getMemory();
        memory.clear();
        if (startStep.equals(Step.ROOT_STEP_NAME)) {
            // Add the system contexts;
            PromptGenerator pg = new PromptGenerator();
            List<String> systemInstructions = pg.getInitialStepSystemInstructionsForRoot(_mContext, _mToolNames,
                    _mAgentCapabilities, _mVariableDefs);
            // For root step, we will have additional information.
            List<String> additionalContextInstructions = sg.getStep(startStep).additionalContextInstructions;
            if (additionalContextInstructions != null) {
                // This is dynamic additional instructions.
                LOGGER.info("Adding additional instructions. Size {}",systemInstructions.size());
                systemInstructions.addAll(additionalContextInstructions);
            }
            memory.addContent(null);
            for (String m : systemInstructions) {
                memory.addContent(new ModelSystemMessage(m));
            }
        } else {
            // Only domain specifc instructions will be added.
            PromptGenerator pg = new PromptGenerator();
            List<String> systemInstructions = pg.getInitialStepSystemInstructionsForDomain(_mContext, _mToolNames,
                    _mAgentCapabilities, _mVariableDefs);
            memory.addContent(null);
            for (String m : systemInstructions) {
                memory.addContent(new ModelSystemMessage(m));
            }
        }
        runner.runStep(startStep);
        return runner;
    }

    public List<String> getInitialStepSystemInstructionsForDomain() {
        PromptGenerator pg = new PromptGenerator();
        return pg.getInitialStepSystemInstructionsForDomain(_mContext, _mToolNames,
                _mAgentCapabilities, _mVariableDefs);
    }

    public MCPServerInstance getMCPServerInstance(String name) {
        return _mMCPServers.get(name);
    }

    public ObjectNode getAgentDiscoveryJSON(boolean extended) {
        ObjectNode on = JsonUtils.MAPPER.createObjectNode();
        on.put("name", this.name);
        on.put("id", this.id);
        on.put("description", this.description);
        on.put("version", "1.0.0");

        if (extended) {
            String url = AGENT_BASE_URL + "/" + this.name;
            on.put("endpoint_url", url);

            // For now use only one skil
            ArrayNode skills = JsonUtils.MAPPER.createArrayNode();
            ObjectNode skill = JsonUtils.MAPPER.createObjectNode();
            skill.put("name", name);
            skill.put("description", description);
            skills.add(skill);
            on.set("skills", skills);
        }

        return on;
    }

    List<String> getAvailableAgentIds() {
        return _mAgentIds;
    }

    void setAvailableAgentFor(String id, AgentCard availableAgent) {
        _mAvailableAgentCards.put(id, availableAgent);
    }

    // This is just a wrapper class
    private class AgentCommChannel {

        private String _mChannelInstanceId = null;
        private String _mChannelKey = null;
        private String _mChannelName = null;
        private boolean _mHasErrors = false;

        public AgentCommChannel(String instanceId, String channelKey, String name, boolean hasErrors) {
            _mChannelInstanceId = instanceId;
            _mChannelKey = channelKey;
            _mChannelName = name;
            _mHasErrors = hasErrors;
        }

        public ObjectNode getJSON() {
            ObjectNode on = JsonUtils.MAPPER.createObjectNode();

            on.put("channelName", _mChannelName);
            on.put("instanceId", _mChannelInstanceId);
            on.put("channelKey", _mChannelKey);
            on.put("hasErrors", _mHasErrors);

            return on;
        }

    }

}
