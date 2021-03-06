package com.greenskinmonster.a51nb.okhttp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by GreenSkinMonster on 2016-12-04.
 */

public class ParamsMap {
    private Map<String, List<String>> params = new HashMap<>();

    public void put(String key, int value) {
        put(key, String.valueOf(value));
    }

    public void put(String key, String value) {
        if (key == null)
            return;

        if (!params.containsKey(key))
            params.put(key, new ArrayList<String>());

        params.get(key).add(value != null ? value : "");
    }

    public List<String> get(String key) {
        return params.get(key);
    }

    public Set<String> keySet() {
        return params.keySet();
    }

    public Set<Map.Entry<String, List<String>>> entrySet() {
        return params.entrySet();
    }
}
