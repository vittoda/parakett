package com.parakett.cli;

import java.util.Date;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.metrics.MetricsDB;
import com.parakett.metrics.MetricsException;
import com.parakett.models.ModelConnection;
import com.parakett.models.ModelConnectionRegistry;
import com.parakett.ws.ClientSession;

public class ModelCommand extends CliCommand {

    public ModelCommand() {
        super("model");

    }

    @Override
    public CliResponse run(String params, ClientSession session) throws CliException {
        ObjectNode resp = JsonUtils.MAPPER.createObjectNode();

        int subCommandIndex = params.indexOf(" ");
        String subCommand = "list";
        if (subCommandIndex > 0) {
            subCommand = params.substring(0, subCommandIndex);
            params = params.substring(subCommandIndex + 1);
        } else if (params.length() > 0) {
            subCommand = params;
            params = "";
        }

        switch (subCommand) {
            case "metrics": {
                return new CliResponse(processMetricsCommand(params));
            }
            case "list": {

                // Get session Ids
                ArrayNode list = ModelConnectionRegistry.getModelNameList();

                resp.put("status", "success");
                resp.put("type", "list");
                resp.set("message", list);

                return new CliResponse(resp);
            }
            case "default": {
                if (params.length() == 0) {
                    // Return the default
                    resp.put("status", "success");
                    resp.put("message", ModelConnectionRegistry.getDefaultModelName());
                } else {
                    // We are setting.
                    ModelConnection conn = ModelConnectionRegistry.getConnectionForModel(params);
                    if (conn == null) {
                        resp.put("status", "error");
                        resp.put("message", "Model '" + params + "' not available.");
                    } else {
                        ModelConnectionRegistry.setDefaultModel(params);
                        resp.put("status", "success");
                        resp.put("message", "Model '" + params + "' set as default.");
                    }
                }

                return new CliResponse(resp);
            }
        }

        resp.put("status", "error");
        resp.put("message", "Invalid model command.");

        return new CliResponse(resp);
    }

    private ObjectNode processMetricsCommand(String params) throws CliException {
        ObjectNode resp = JsonUtils.MAPPER.createObjectNode();

        if (params.length() == 0) {
            resp.put("status", "error");
            resp.put("message", "Provide a model name.");
        } else {
            ModelConnection conn = ModelConnectionRegistry.getConnectionForModel(params);
            if (conn == null) {
                resp.put("status", "error");
                resp.put("message", "Model '" + params + "' not available.");
            } else {
                resp.put("status", "success");

                ArrayNode multiMetric = JsonUtils.MAPPER.createArrayNode();

                ObjectNode metric = JsonUtils.MAPPER.createObjectNode();
                multiMetric.add(metric);
                metric.put("title", "Token consumption");
                metric.put("type", "table");
                ArrayNode cols = JsonUtils.MAPPER.createArrayNode();
                cols.add("period");
                cols.add("inputTokens");
                cols.add("outputTokens");
                ObjectNode formatHints = JsonUtils.MAPPER.createObjectNode();
                formatHints.set("columns", cols);
                metric.set("formatHints", formatHints);
                metric.set("message", getModelTokenMetrics(params));

                metric = JsonUtils.MAPPER.createObjectNode();
                multiMetric.add(metric);
                metric.put("title", "Requests");
                metric.put("type", "table");
                cols = JsonUtils.MAPPER.createArrayNode();
                cols.add("count");
                cols.add("maxDuration");
                cols.add("minDuration");
                 cols.add("avgDuration");
                formatHints = JsonUtils.MAPPER.createObjectNode();
                formatHints.set("columns", cols);
                metric.set("formatHints", formatHints);
                metric.set("message", getModelRequestMetrics(params));

                resp.put("type", "multi");
                resp.set("message", multiMetric);
            }
        }

        return resp;
    }

    private ArrayNode getModelTokenMetrics(String modelName) throws CliException {
        ArrayNode ret = JsonUtils.MAPPER.createArrayNode();
        try {
            ObjectNode o = MetricsDB.getTotalMetricForModel(modelName);
            o.put("period", "Overall");
            ret.add(o);
            o = MetricsDB.getTotalMetricForModelForDate(modelName, new Date());
            o.put("period", "Today");
            ret.add(o);
            o = MetricsDB.getTotalMetricForModelFrom(modelName, ModelConnectionRegistry.STARTUP_TIME);
            o.put("period", "Since restart");
            ret.add(o);
        } catch (MetricsException e) {
            throw new CliException(e);
        }

        return ret;
    }

    private ArrayNode getModelRequestMetrics(String modelName) throws CliException {
        ArrayNode ret = JsonUtils.MAPPER.createArrayNode();
        try {
            ObjectNode o = MetricsDB.getTotalRequestMetricForModel(modelName);
            o.put("period", "Overall");
            ret.add(o);
            o = MetricsDB.getRequestMetricForModelForToday(modelName, new Date());
            o.put("period", "Today");
            ret.add(o);
            ret.add(o);
        } catch (MetricsException e) {
            throw new CliException(e);
        }

        return ret;
    }

}
