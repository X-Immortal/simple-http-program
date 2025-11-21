package com.server;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private String body;

    public HttpRequest() {
        headers = new HashMap<>();
    }

    // getters and setters
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getHeader(String name) { return headers.get(name); }
    public void setHeader(String name, String value) { headers.put(name, value); }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
