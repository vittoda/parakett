# Local Key Manager
To make development and local testing easier, we have an implementation to access the access token or other credentials from a local store. `.fskeys` file on your home directory has all the keys. These keys can be used by Parakett Server, MCP Servers and Channels. This is a simple key value pair file. 

Here is a sample file.

```
parakett.gemini=AQ.Abcdefghijkl-mnopqrst
slack.config={"webhookURL":"https://hooks.slack.com/services/ABCDEFGHIJKL/AAAABBBCCC/abcdefghijklmno"}
```

Please note that, the access tokens are stored as plain text. So, do not store access tokens that you don't want someone having access to your home directory see it. We do have plan to have more secure mechanism and will be implemented in near puture.