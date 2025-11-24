package HTTP.server;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private int statusCode;
    private String statusMessage;
    private Map<String, String> headers;
    private String body;

    public HttpResponse() {
        headers = new HashMap<>();
        statusCode = 200;
        statusMessage = "OK";
        setContentType("text/html");
    }

    // getters and setters
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public String getHeader(String name) { return headers.get(name); }
    public void setHeader(String name, String value) { headers.put(name, value); }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getContentType() { return getHeader("Content-Type"); }
    public void setContentType(String contentType) { setHeader("Content-Type", contentType); }
}
