package com.parakett.ws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WSClientSessionCache {

    private static Map<String, ClientSession> _mClientSessions = new ConcurrentHashMap<>();

    public static void add(ClientSession cl) {
        _mClientSessions.put(cl.clientId, cl);
    }

    public static ClientSession get(String clientId) {
        return _mClientSessions.get(clientId);
    }

    public static void remove(String clientId) {
        _mClientSessions.remove(clientId);
    }
    
}
