package utils;

import org.json.JSONObject;

public class JSON {
    private final JSONObject json;

    public JSON() {
        json = new JSONObject();
    }

    public JSON(String json) {
        this.json = new JSONObject(json);
    }

    public boolean contains(String key) {
        return json.has(key);
    }

    public String get(String key) {
        return json.getString(key);
    }

    public void add(String key, String value) {
        json.put(key, value);
    }

    public byte[] getBytes() {
        return EncodingUtil.encodeText(json.toString());
    }

    @Override
    public String toString() {
        return json.toString();
    }
}
