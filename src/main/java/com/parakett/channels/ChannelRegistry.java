package com.parakett.channels;

import java.util.HashMap;
import java.util.ServiceLoader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.parakett.channels.base.CommChannelBase;
import com.parakett.channels.base.CommChannelException;
import com.parakett.channels.base.CommChannelInstance;
import com.parakett.channels.base.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelRegistry.class);

    private static final HashMap<String, CommChannelBase> _mRegisteredChannels = new HashMap<>();
    private static final HashMap<String, JsonNode> _mChannelDefinitions = new HashMap<>();

    public static void loadChannels() {
        // ServiceLoader looks for files in META-INF/services
        ServiceLoader<CommChannelBase> loader = ServiceLoader.load(CommChannelBase.class);

        for (CommChannelBase channel : loader) {
            String key = channel.getKey();
            LOGGER.info("Regestering channel '{}'", key);
            _mRegisteredChannels.put(key, channel);
        }
    }

    public static CommChannelInstance createInstanceFor(String channelInstanceKey, String agentKey) {
        JsonNode channelInstanceDef = _mChannelDefinitions.get(channelInstanceKey);
        if(channelInstanceDef == null) {
            LOGGER.error("Channel instance config is not defined for instance id "+channelInstanceKey);
            return null;
        }
        String channelId = channelInstanceDef.get("channel").asText();
        CommChannelBase channel = _mRegisteredChannels.get(channelId);
        if (channel == null) {
            return null;
        }

        JsonNode config = null;
        if(channelInstanceDef.has("config") && !channelInstanceDef.get("config").isNull()) {
            config = channelInstanceDef.get("config");
        }
        return channel.createInstance(agentKey, config);
    }

    public static CommChannelBase getChannelFor(String channelInstanceId) {
        JsonNode on = _mChannelDefinitions.get(channelInstanceId);
        return _mRegisteredChannels.get(on.get("channel").asText());
    }

    public static void loadChannelDefinitions(String file) throws CommChannelException{
        try {
            InputStream is = new FileInputStream(file);

            JsonNode o = JsonUtils.MAPPER.readTree(is);
            ArrayNode channelInstances = (ArrayNode) o.get("channelInstances");
            int len = channelInstances.size();
            for (int i = 0; i < len; i++) {
                JsonNode channelInstanceConfig = channelInstances.get(i);
                String channelInstanceId = channelInstanceConfig.get("id").asText();
                _mChannelDefinitions.put(channelInstanceId, channelInstanceConfig);
            }

        } catch (IOException e) {
            throw new CommChannelException(e);
        }
    }

}