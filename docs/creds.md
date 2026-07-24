# Credentials and Access tokens

This document outlines the process for obtaining the access tokens and credentials required to connect with communication channels like Gmail and Slack, as well as MCP servers including Google Calendar and Location IQ. While setting up enterprise-level credentials typically involves a different set of instructions, the steps provided here focus strictly on the configuration needed to run the basic framework examples. 

## Google

Following instructions are common for Gmail, Google Calender and Google Drive. We will start the process Gmail and latter follow similar steps for other services.

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project (or use an existing one)
3. Enable the Gmail API
    1. On the left navigation panel, click **APIs and Services**, followed by **Enabled APIs and services**.
    2. At the top click **+ Enable APIs and services** link.
    3. Search for *Gmail*, and select the *Gmail APIs*
    4. Click Enable button
4. Generate **OAuth 2.0 Client Credentials**
    1. On the screen for Gmail API, click **Create Credentials**
    2. Ensure API is *Gmail API*.
    3. Select **User Data** for type
    5. Click **Save And Continue***
    6. In the OAuth consent screen, select a name for an app. For example *Parakett*
    7. In **User support email** enter your email address. You can enter the same for **Developer contact information**
    8. You can leave the scope for now. Click **Save and Continue**
    9. In **OAuth Client ID**, select *Desktop app* as **Application type* and a name for the client. Say *Parakett Client*
    10. Click **Create*
    12. In the next click click Download button to download the credentials file to a safe location.
5. Add email for your test app. We are not publising the app we have created above (as this is only for testing. If you chose you can do so.) So, we need add test users. Otherwise the following steps will fail.
    1. o to the [Google Cloud Console](https://console.cloud.google.com/)
    2. On the left navigation panel, click **APIs and Services**, followed by, and then **OAuth consent screen**
    3. On the left navigation bar, panel **Audience**.
    4. Scroll down, and in **Test users** section , click **Add users**.
    5. Enter your email in the popup window and click **Save**
6. Now you need to generate, access tokens and refresh tokens. The MCP server has mechanisms to generate this for you.
    1. Go to gmail MCP server
    2. Run the following command (Replace the jar file with appropriate one)

        ```
        java -jar gmail-1.0.1-all.jar auth <your_credentials_file>
        ```

    3. This will open a browser window select and authenticate your account.
    4. You will see a warning message that. Google hasn't verified this app. Confirm that you are invoted by the right developer, and click **Continue**
    5. Click **Select all**
    6. Click **Save**.
    7. This will generate access tokens in `~/.fskeys` file. If the specific key exists, it will update the specific key. Keys for access tokens will `google.gmail.tokens` and keys for client credentials will be `google.gmail.clientCreds`

7. To generate the access tokens for Google Calender, you need to use the Google Calender MCP server and following similar steps as in 6. It will generate seperate access tokens and client credentials will appropriate scope. Key will be `google.gcal.clientCreds` and `google.gmail.tokens`
8. For Google Drive, access token key will be `google.gdrive.tokens` and client credential keys will be `google.gdrive.clientCreds`

## LocationIQ

[LocationIQ](https://locationiq.com/) provides forward and reverse gecoding capabilities. You can create a free account.

1. Click **START FOR FREE** on home page and login.
2. You will *Playground*. If not use [https://my.locationiq.com/](https://my.locationiq.com/)
3. Click on **Access Tokens** on the left navigation panel.
4. It will show all available access tokens. Default one will be created for you.
5. Click on the link to see the access token. You can save the same in `~/.fskeys` file using the key `locationiq.key`

## Slack

We are using slack as MCP server and input as a channel. So, we need to following various steps mentioned below to get the credentials, create an app. In the following set of instructions, we will assume that you don't have slack workspace yet and starting from a fresh account. If you already have one, skip the steps accordingly

1. Login to [slack](https://slack.com/signin#/signin)
2. Create a workspace when prompted to. Say `Parakett`.
3. Go ahead and create a new slack channel. Same it ias `Parakett` or whatever you prefer.

### Create webhook URL
Webhook URL is needed by slack MCP server 

1. Goto [https://api.slack.com/apps](https://api.slack.com/apps)
2. Click **Create an App**. Select **From Scratch** option
3. Let us name the app as `Parakett` as well. Select the channel you created in the above step.
4. Click **Create App** button. (Follow the terms and conditions.)
5. Once the app is created, on the left navigation panel click **Incoming Webhooks** under **Features** tab.
6. Toggle the **Activate Incoming Webhooks**
7. At the bottom click **Add New Webhook**. 
8. In the next window **Allow the app to access Slack**, select the workspace and channel you have created above and click **Allow**
9. Copy the webhook URL and add to `~/.fskey`. Use `slack.config` as the key and value as `{webhookURL:"yourwebhook URL"}`

### Create Bot token and App token

We will need Bot token and App token to read messages from MCP server or channels (which uses Slack SDK). We will use the same app we create above

1. Goto [https://api.slack.com/apps](https://api.slack.com/apps)
2. Ensure that the app you created above is selected
3. From the **Settings** tab, click **Socket Mode**.
4. Toggle **Enable Socket Mode**. It wil popup a window. Give a token name. 
5. Click **Generate**. This will generate *app token*. Copy it and note it at a safe place. Click **Done**.
6. Click **Event Subscriptions** on the left navigation bar, under **Features** tab.
7. Toggle **Enable Events**. 
8. Scroll down and expand **Subscribe to bot events** and click **Add Bot User Event**.
9. Search and add the following events
    * `message.channels`
    * `message.groups`
    * `message.im`

10. Click **save Changes**. 
11. You will see a notification to **reinstall the app**. Click the link to reinstall. 
12. Now you need permissions to write, and read user internal Id from their email address. Click **OAuth & Permissions**.
13. Scroll down to the bottom under **Scopes** and then **Bot Token Scopes**, click **Add on OAuth Scope**, and add the following scopes.
    * `chat:write`
    * `users:read.email`
    * `users:read`

14. Re-install the app
15. Once the app is re-installed , click **Install App** on **Settings** tab, and copy the Bot token. Save it at a safe place.
16. Add the app to the channel.
    1. Goto the channel settings
    2. Click **Agent & Apps** tab
    3. Click **Add Agent & Apps**. 
    4. Select the Parakett app and add it.
17. In `~/.fskeys`, ass a key `slack.channel.config` with value as `{appToken:'App token', botToken:' Bot auth token'}`.


