package com.parakett;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.parakett.agent.AgentRegistry;
import com.parakett.channels.ChannelRegistry;
import com.parakett.cli.AgentCliCommand;
import com.parakett.cli.CliCommandRegistry;
import com.parakett.cli.HelpCommand;
import com.parakett.cli.ModelCommand;
import com.parakett.cli.SessionsCliCommand;
import com.parakett.flow.FlowExecutionQueue;
import com.parakett.mcp.MCPRegistry;
import com.parakett.metrics.MetricsDB;
import com.parakett.models.ModelConnectionRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {

            String mcpBase = System.getProperty("mcp.base", null);
            if (mcpBase == null) {
                mcpBase = System.getProperty("user.home") + "/projects/agent/mcp";
                System.setProperty("mcp.base", mcpBase);
            }

            String mcpConfigFile = System.getProperty("fs.mcpConfigFile");
            if (mcpConfigFile == null) {
                mcpConfigFile = "." + File.separator + "mcpServers.json";
            }

            if (!Files.exists(Paths.get(mcpConfigFile))) {
                LOGGER.error(
                        "MCP config file is not defined. Use system property 'fs.mcpConfigFile' to set the MCP config file, or, save the config file as 'mcpServers.json' in current folder.");
                return;
            }

            String channelInstanceDefinitionFile = System.getProperty("fs.channelsConfigFile");
            if (channelInstanceDefinitionFile == null) {
                channelInstanceDefinitionFile = "." + File.separator + "channelsConfig.json";
                if (!Files.exists(Paths.get(channelInstanceDefinitionFile))) {
                    LOGGER.warn(
                            "Channel instance configuration file '{}' does not exist. Channels will not be avaolable.",
                            channelInstanceDefinitionFile);
                    channelInstanceDefinitionFile = null;
                    return;
                }
            } else {
                // Check if the file exists
                if (!Files.exists(Paths.get(channelInstanceDefinitionFile))) {
                    LOGGER.error("Channel instance configuration file '{}' does not exist.",
                            channelInstanceDefinitionFile);
                    return;
                }
            }

            String agentsConfigFile = System.getProperty("fs.agentsConfigFile");
            if (agentsConfigFile == null) {
                agentsConfigFile = "." + File.separator + "agents.json";
            }

            if (!Files.exists(Paths.get(agentsConfigFile))) {
                LOGGER.error(
                        "[ERROR] Agent config file is not defined. Use system property 'fs.agentsConfigFile' to set the Agents config file, or, save the config file as 'agents.json' in current folder.");
                return;
            }

            // Load Keys
            Keys.loadKeys();

            ModelConnectionRegistry.initialize();
            MetricsDB.initialize();

            // Load the MCP servers and discover the tools.
            LOGGER.info("Loading MCP config file '{}'", mcpConfigFile);
            MCPRegistry.initialize(mcpConfigFile);

            if (channelInstanceDefinitionFile != null) {
                LOGGER.info("Loading Channel instances config file '{}'", channelInstanceDefinitionFile);
                ChannelRegistry.loadChannelDefinitions(channelInstanceDefinitionFile);
            }

            FlowExecutionQueue.INSTANCE.startConsumer();
            ChannelRegistry.loadChannels();
            SpringApplication.run(App.class, args);

            LOGGER.info("Loading Agent config file '{}'", agentsConfigFile);
            AgentRegistry.loadFromFile(agentsConfigFile);

            // Register commands
            CliCommandRegistry.addCommand(new AgentCliCommand());
            CliCommandRegistry.addCommand(new SessionsCliCommand());
            CliCommandRegistry.addCommand(new HelpCommand());
            CliCommandRegistry.addCommand(new ModelCommand());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
