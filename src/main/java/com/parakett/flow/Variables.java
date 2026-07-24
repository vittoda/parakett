package com.parakett.flow;

import java.util.HashMap;
import java.util.Set;

public class Variables {

    private HashMap<String, Variable> _mValues = new HashMap<>();

    public void setValue(String key, Object value) {
        _mValues.put(key, new Variable(key, value));
    }

    public boolean has(String key) {
        return _mValues.containsKey(key);
    }

    public Object getValue(String key) throws NullPointerException {
        if(_mValues.containsKey(key)) {
            return _mValues.get(key).value;
        }

       throw new NullPointerException("No value found for '"+key+"'");
        
    }

    public int size() {
        return _mValues.size();
    }

    public Set<String> keys() {
        return _mValues.keySet();
    }
}
