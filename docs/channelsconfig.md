# Channels Configuration

This configuration defines channel instance configuration. For example, your agent may use two instances of slack. This is where you define them and each having its own configuration. If two agents share the same configuration, you can define one instance here and refer the same Id in agent definition. Internally, server will create one instance each for each of the agent. It is up to channel implementation, how it handles the instances with same configuration.

```
{
    channelInstances : ChannelInstance[]
}
```


## Channel instanc configuration

```
{
    "channel": "slack",
    "id": "slack",
    "config": ChannelConfig
}
```

### Details
| Field Name | Type | Mandatory | Description |
| :--- | :---: | :---: | :--- |
| `name` | String | Yes | A unique identifier for the server. Agents will use this value to identify the MCP server it needs to use |
| `category` | String | Yes | The functional classification. At the moment used for UI rendering. Supported values are `DB`, `System` and `Saas` |
| `command` | String | Yes | Command that will be executed by the Parakett server to start the MCP server. You can use system property `mcp.base`, as part of the command|
| `connection` | Object | Yes | At the moment only *stdio* mode is available for MCP servers. So, the *type* attribute will have `stdio` as the value