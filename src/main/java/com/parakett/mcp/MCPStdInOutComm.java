package com.parakett.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPStdInOutComm extends MCPComm {

    private static final Logger LOGGER = LoggerFactory.getLogger(MCPStdInOutComm.class);

    private String _mCommand = null;
    private Process _mProcess = null;
    private OutputStream _mOutputStream = null;
    private InputStream _mInputStream = null;
    private InputStream _mErrorStream = null;
    private BufferedReader _mReader = null;
    private String _mServerName = null;

    public MCPStdInOutComm(String command, String serverName) {
        _mCommand = command;
        _mServerName = serverName;
    }

    @Override
    public JsonNode sendRequest(ObjectNode request) throws MCPException {
        if (_mProcess == null) {

            try {
                String[] parts = _mCommand.split("\\s+");
                // Replace variable.
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if (part.contains("${")) {
                        Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(part);
                        StringBuilder sb = new StringBuilder();
                        while (matcher.find()) {
                            String value = System.getProperty(matcher.group(1));
                            matcher.appendReplacement(sb,
                                    value != null ? Matcher.quoteReplacement(value) : "<variable_undefined>");
                        }
                        matcher.appendTail(sb);
                        parts[i] = sb.toString();
                    }
                }
                _mProcess = new ProcessBuilder(parts).start();
                _mOutputStream = _mProcess.getOutputStream();
                _mInputStream = _mProcess.getInputStream();
                _mReader = new BufferedReader(new InputStreamReader(_mInputStream));
                _mErrorStream = _mProcess.getErrorStream();

            } catch (IOException e) {
                throw new MCPException(e);
            }
        }

        try {
            String json = JsonUtils.MAPPER.writeValueAsString(request);
            _mOutputStream.write((json + "\n").getBytes(StandardCharsets.UTF_8));
            _mOutputStream.flush();

            return readResponse();

        } catch (IOException e) {
            drainErrorStream(_mErrorStream);
            throw new MCPException(e);
        }
    }

    public JsonNode readResponse() throws IOException {
        String line = _mReader.readLine();

        if (line == null) {
            throw new IOException("MCP process output stream closed (EOF)");
        }

        line = line.trim();
        if (line.isEmpty()) {
            return readResponse(); // skip empty lines
        }

        try {
            return JsonUtils.MAPPER.readTree(line);
        } catch (Exception e) {
            throw new IOException("Invalid JSON from MCP: " + line, e);
        }
    }

    public void drainErrorStream(InputStream errorStream) {
        try {
            InputStreamReader isr = new InputStreamReader(errorStream);
            BufferedReader reader = new BufferedReader(isr);

            while (errorStream.available() > 0) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                LOGGER.error("Error line from MCP server '{}' : {} ",_mServerName , line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws MCPException {
        try {
            _mOutputStream.close();
        } catch (IOException e) {
            throw new MCPException(e);
        }
    }

}
