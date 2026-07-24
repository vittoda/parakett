# Approval workflow

This example will demonstrate following capabilities of Parakett server.

1. Work with multiple MCP servers
2. Multiple workflow in the same agent
3. Multiple input channels (CLI and Gmail)

This agent manages the approval process for a team event using the following workflow:

1. **Event Submission:** The user submits a prompt containing the event details (date, time, and location).
2. **Approval Request:** The agent sends an approval request email to the reviewer.
3. **Approver Response:** The reviewer approves or rejects the request directly via email.
4. **Calendar & Database Update:** Once the agent receives the approval email, it adds the event to the calendar and updates the event's lifecycle status in a SQLite database.

Following MCP servers are used
* SQLite
* LocationIQ
* GMail
* Google Calender

## Instructions
1. Ensure that the MCP servers, channels, Parakett server and Parakett UI are built. Follow the instructions in the respective README.md for the same
2. Ensure you have credentials for Gmail, Google Calender generated and placed appropriately. Follow the instructions in the [documentation](https://vittoda.github.io/parakett/creds/) for credentials and configuration 
3. Update the approvals.md. This file is the domain context for approval life cycle. It is created in markdown format. In this file make the following changes
    1. Replace `<senderEmail>` with your email id  (Do not remove the back-quote). Your Gmail credentials should have permissions to send email on belaf of this email id
    2. Replace `<approverEmail>`, with approver email (Do not remove the back-quote). They should be differrent, otherwise the event will be triggered incorrectly.

4. Start the server (replace `mcp_server_folder` with MCP servers root folder)
    ```
   java -Dfs.mcpConfigFile=mexamples/approvals/mcpServers.json \
       -Dfs.agentsConfigFile=mexamples/approvals/agents.json \
       -Dfs.channelsConfigFile=mexamples/approvals/channelsConfig.json \
       -Dmcp.base=<mcp_server_folder> \
       -jar build/libs/parakett-0.0.1.jar
    ```

5. Start the Parakett UI.

    ```npm run dev```

6. Enter the following prompt in the web CLI interface in Parakett UI , after connecting to server.

    ```
    agent run approvalsAgent --log --archive   I need to setup a team dinner event on 30th July 2026, at 8:30 PM, location: Gatsby Cocktails & Cuisines, Bannerghatta Road, Bangalore , Karnataka, India. Get the required approvals
    ```

    Replace the restaurent name as appropriate.

    In few minutes you will recieve email in approver email id.

7. Reply 'Approved' to the approval email. 
8. Once the agent recieves the email, it will trigger a flow, which will update SQLite DB and add an event to the calender.

### Note:
1. Gmail channel will poll the events every minute for messages in last 1 minute. So, there might be some overlap. During this testing, once the agent processed the approval email, delete it to avoid re-triggering again.
2. Do not use single email Id for approver and requester. The way emails are filtered in the channel, may trigger incorrect second flow.