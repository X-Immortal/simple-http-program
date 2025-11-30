package HTTP.client;

import HTTP.message.HTTPResponse;

public class File {
    private final byte[] data;
    private final String timestamp;
    private final HTTPResponse response;

    public File(HTTPResponse response) {
        this.response = response;
        this.data = response.getBody().getBytes();
        this.timestamp = response.getHeaders().get("Last-Modified");
    }

    public byte[] getData() {
        return data;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public HTTPResponse getResponse() {
        return response;
    }
}
