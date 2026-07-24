# Parakett Configurations

This document covers configurations for Parakett server. Schema in the following sections will use `Typescript` format to define the type.

## MCP Server configuration.

MCP servers configuration file defines list of MCP servers available for the Parakett instance. Here is the structure of configuration file

```
{
    mcpServers : [ 
        <MCP server configuration>
    ]
}
```

Each of the MCP server definition follows the following structure

```javascript   
{
    "name": string,
    "category" : "DB" | "System" | "Saas",
    "command" : string,
    "connection": {
        "type": "stdio"
    }
}
```

### Details
| Field Name | Type | Mandatory | Description |
| :--- | :---: | :---: | :--- |
| `name` | String | Yes | A unique identifier for the server. Agents will use this value to identify the MCP server it needs to use |
| `category` | String | Yes | The functional classification. At the moment used for UI rendering. Supported values are `DB`, `System` and `Saas` |
| `command` | String | Yes | Command that will be executed by the Parakett server to start the MCP server. You can use system property `mcp.base`, as part of the command|
| `connection` | Object | Yes | At the moment only *stdio* mode is available for MCP servers. So, the *type* attribute will have `stdio` as the value

Here is a sample for SQLite MCP server

```
{
    "name": "sqlite",
    "category" : "System",
    "command" : "/usr/bin/java -DdataPath=/tmp -jar ${mcp.base}/sqlite/build/libs/sqlite-1.0.1-all.jar ",
    "connection": {
        "type": "stdio"
    }
}
```

## Agent configuration.
Agent configuration defines agent instances, which are combination of prompts, MCP servers, channels. Agent configuraiton will have similar top level structure as MCP servers.

```
{
    agents : [ 
        <Agent definition>
    ]
}
```

Each of the agent definition has the following schema

```
{
    "name": string,
    "id": string,
    "description": string,
    "context": {file:string} | string,
    "mcpServers": string[],
    "channels": string[],
    "variables": Variable[],
    "hitlConfig" : HITLConfig[]
}
```

### Agent Definition
| Field Name | Type | Mandatory | Description |
| :--- | :---: | :---: | :--- |
| `name` | String | Yes | A unique display name for the Agent. |
| `id` | String | Yes | Unique identifier for the agent. Within Parakett or for multi-agent configuration, this will be the identifier used. Reccomended characeters are A-Z,a-z,0-9,_. |
| `description` | String | Yes | A free form text describing the agent. This information will be used by other agents to determine high level capabilities of this agent. |
| `context` | Object | Yes | Domain context for the agent, which provides system context for the LLM for this agent. Value can be a context. If the context is too large, save it in a file and specify the `file` attribute for context. |
| `mcpServers` | String[] | Yes | An array of MCP server identifiers that this agent will use. |
| `agents` | String[] | No | An array of agent identifiers this agent can communicate to in a multi-agent scneario. |
| `channels` | String[] | No | An array of channels the agent will listen to and respond to. At the moment only `slack` is supported as channel. |
| `variables` | Variable[] | No | List of variables that the agent prompts will use, which will be replaced with values during runtime. See the [Variables section](#variables) |
| `hitlConfig` | Object | No |Human in the loop configuration. See [HITLConfig Section](#hitlconfig-human-in-the-loop) |


### Variables
Variables can be included in system context or user prompts for the agent. They are replaced at runtime before sending the prompt to LLM.

```
{
    "name": string,
    "label": string,
    "description": string,
}
```

| Field Name | Type | Mandatory | Description |
| :--- | :---: | :---: | :--- |
| `name` | String | Yes | Name of the variable. Reccomended characeters are A-Z,a-z,0-9,_.|
| `label` | String | Yes | A display name for the variable. Used primarily in UI|
| `description` | String | Yes | Detailed description of the variable. Used in UI|

### HITLConfig (Human In The Loop)
Human in the loop configuration provides definitions for setup to handle human in the loop events from the agent. Here is the schema for the definition. Basically HITLConfig provides list of targets.

```
{
    "targets" : HITLTarget[]
}
```

### HITLTarget
```
{
    "type" : "webhook" | "event";
    "instance" : string;
}
```
| Field Name | Type | Mandatory | Description |
| :--- | :---: | :---: | :--- |
| `type` | String | Yes | Supports the following options.<br> . `event` : An event will be triggered on the subscribed channels for this target type<br> . `webhook` : A POST request will be triggered on the provided webhook URL. |
| `instance` | String | Only for `webhook` type | URL of the webhook|

Here is a sample agent definition for an agent with Human In The Loop configuration

```
{
    "name": "SQLite",
    "id": "sqlite",
    "description": "SQLite Integration",
    "context": {
        "file": "sqliteContext.txt"
    },
    "hitlConfig" :{
        "targets" : [
            {
                "type" : "webhook",
                "instance" : "http://localhost:8090/hil"
            },
            {
                "type" : "event"
            }
        ]
    },
    "mcpServers": ["sqlite"]
}
```

Following samle agent configuration uses variables.

```
{
    "name": "File system and GMail",
    "id": "fs_and_Gmail",
    "description": "File system and Gmail integration",
    "context": {
        "file": "fileSystemAndGmailSystemPrompt.txt"
    },
    "mcpServers": ["filesystem","gmail"]
    "variables": [
        {
            "name": "fullName",
            "label": "Full Name",
            "description": "Full name of the recipient"
        },
        {
            "name": "subject",
            "label": "Email Subject",
            "description": "Subject for the email"
        },
        {
            "name": "email",
            "label": "Email address",
            "description": "Email address recipient"
        }
    ]
}
```

## Channels Configuration
of slack. This is where you define them and each having its own configuration. If two agents share the same configuration, you can define one instance here and refer the same Id in agent definition. Internally, server will create one instance each for each of the agent. It is up to channel implementation, how it handles the instances with same configuration.

```
{
    "channelInstances" : ChannelInstance[]
}
```

### Channel instance configuration

```
{
    "channel": "slack" | "gmail",
    "id": string,
    "config": ChannelConfig
}
```

| Field Name | Type | Mandatory | Description |
| :--- | :---: | :---: | :--- |
| `channel` | String | Yes | Channel name. Possible values `slack` and `gmail`|
| `id` | String | Yes | A unique id for this channel instance. This identifier will be used by agent definition |
| `config` | String | No | Channel specific configuration|


### Slack channel instance configuration

```
{
    "credentials": {
        "botToken" : string,
        "appToken" : string
    }
}
```

| Field Name | Type | Mandatory | Description |
| :--- | :---: | :---: | :--- |
| `credentials` | String | No | Slack credentials should include `appToken` and `botToken` attributes |
| `credentialsKey` | String | No | If the credentials are not provided in the configuration, this value will specify the key in local key manager. Default value is `slack.channel.config` |

In the configuration, one of `credentials` or `credentialsKey` can be provided.


### Gmail channel instance configuration

```
{
    "accessTokens" : {
        "access_token" : string,
        "refresh_token" : string,
        "expiry" : number
    },
    "clientCreds" : {
        "clientId" : string,
        "clientSecret" : string
    },
    "accessTokenKey" : string,
    "clientCredsKey" : string,
    "filter" : GmailFilter
}
```

| Field Name | Type | Mandatory | Description |
| :--- | :---: | :---: | :--- |
| `accessTokens` | String | Yes | Contains the `access_token`, `refresh_token` and `expiry` attributes. Expiry is the milliseconds since epoch. Google API will return expires field, which gives the seconds till the access token is valid (this is relative). This should be changed to absolute timestamp.|
| `clientCreds` | String | Yes | Client credentials are used for refreshing the access token, along with refresh token when it expires. The object should contain `clientId` and `clientSecret`|
| `accessTokenKey` | String | No | If credentials are not specified, this key points to the access token details in local key manager. If this field is not specified, `google.gmail.tokens` will be used as access token key |
| `clientCredsKey` | String | No | If credentials are not specified, this key points to the client credentials object in local key manager. If this field is not specified, `google.gmail.clientCreds` will be used as access token key |
| `filter` | String | No | This configuration defines filter criteria to process the messages. If no filter is provided, all the incoming messages are processed. |

In the configuration, if `credentials` is not specified, the implementaion will use local key manager to fetch the key

#### GmailFilter

Filter on the incoming messages to processed. You can specify multiple filter conditions. All the conditions should match for the message to be processed.

```
{
    "subject" : {
        "contains" : string,
        "startsWith" : string,
        "endsWith" : string
    },
     "sender" : {
        "contains" : string,
        "startsWith" : string,
        "endsWith" : string
    },
    "body" : {
        "contains" : string
    }
}
```

### Example configuration for channel instances.

Here is an example channel configuration file which uses *Slack* and *Gmail* channels.

```
{
    "channelInstances": [
        {
            "channel": "slack",
            "id": "slack",
            "config": {
                "credentials": {
                    "appToken" : "xapp-1-123456789-12311233-12111221",
                    "botTokenToken" : "xoxb-1-123456789-12311233-12111221"
                }
            }
        },
        {
            "channel": "gmail",
            "id": "gmail",
            "config": {
                "accessTokenKey": "google.gmail.tokens",
                "clientCredsKey": "google.gmail.clientCreds",
                "filter" : {
                    "subject" : {
                        "contains" : "Parakett Request"
                    }
                }
            }
        }
    ]
}
```
