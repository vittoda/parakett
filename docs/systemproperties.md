#Parakett Server startup system properties

Parakett server during startup accepts few (optional) system properties that defines the behavior of the system.

* `mcp.base` : Used in MCP Server command. This can be a component of the path. In case you don't want to expose local path in the configuration, this will be a useful option. If not specified, this will degault to `{user.home}/projects/agent/mcp`
* `fs.mcpConfigFile` : This property defines the MCP server configuration. If not specified, `mcpServers.json` from the current working directory will be used.
* `fs.agentsConfigFile` : This property defines the agent definitions. If not specified, `agents.json` from the current working directory will be used.
* `fs.channelsConfigFile` : This configuration file defines channel instance configurations. If not specified `channelsConfig.json` from the current directory will be used.