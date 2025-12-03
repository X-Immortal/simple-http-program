package HTTP.message;

import HTTP.message.exception.HTTPResponseFormatException;
import HTTP.message.exception.HTTPResponseHeadersFormatException;
import HTTP.message.exception.HTTPStatusLineFormatException;
import HTTP.rule.HTTPVersion;
import HTTP.utils.EncodingUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class HTTPResponse {
    private HTTPStatusLine statusLine;
    private HTTPResponseHeaders headers;
    private HTTPResponseBody body;

    public static class HTTPStatusLine {
        private static final HashMap<Integer, String> statusCodes = new HashMap<>();

        private String version;
        private int statusCode;
        private String statusMessage;
        private boolean modifiable;

        static {
            statusCodes.put(200, "OK");
            statusCodes.put(301, "Moved Permanently");
            statusCodes.put(302, "Found");
            statusCodes.put(304, "Not Modified");
            statusCodes.put(400, "Bad Request");
            statusCodes.put(401, "Unauthorized");
            statusCodes.put(404, "Not Found");
            statusCodes.put(405, "Method Not Allowed");
            statusCodes.put(409, "Conflict");
            statusCodes.put(500, "Internal Server Error");
        }

        public HTTPStatusLine() {
            modifiable = true;
        }

        public HTTPStatusLine(String statusLine) throws HTTPStatusLineFormatException {
            statusLine = EncodingUtil.binaryToText(statusLine);
            String[] parts = statusLine.split(" ", 3);
            if (parts.length != 3) {
                throw new HTTPStatusLineFormatException("Lack necessary parts");
            }

            modifiable = true;
            setVersion(parts[0]);
            setStatusCode(Integer.parseInt(parts[1]));
            modifiable = false;
        }

        public void setVersion(String version) throws HTTPStatusLineFormatException {
            if (!modifiable) return;
            if (!HTTPVersion.support(version)) {
                throw new HTTPStatusLineFormatException("HTTP version is not supported: " + version);
            }
            this.version = version;
        }

        public void setStatusCode(int statusCode) throws HTTPStatusLineFormatException {
            if (!modifiable) return;
            if (!statusCodes.containsKey(statusCode)) {
                throw new HTTPStatusLineFormatException("HTTP status code is not supported: " + statusCode);
            }
            this.statusCode = statusCode;
            this.statusMessage = statusCodes.get(statusCode);
        }

        public String getVersion() {
            return version;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public byte[] getBytes() {
            return EncodingUtil.encodeText(String.join(" ", version, String.valueOf(statusCode), statusMessage));
        }
    }

    public static class HTTPResponseHeaders {
        private static final String KEY_REGEX = "[A-Za-z]+(-[A-Za-z]*)*";

        private final HashMap<String, String> fields = new HashMap<>();
        private boolean modifiable;

        public HTTPResponseHeaders() {
            modifiable = true;
        }

        public HTTPResponseHeaders(String headers) throws HTTPResponseHeadersFormatException {
            headers = EncodingUtil.binaryToText(headers);
            modifiable = true;
            parse(headers);
            modifiable = false;
        }

        private void parse(String headers) throws HTTPResponseHeadersFormatException {
            String[] fields = headers.split("\r\n");
            for (String field : fields) {
                String[] kv = field.split(": ", 2);
                if (kv.length != 2) {
                    throw new HTTPResponseHeadersFormatException("Invalid field: " + field);
                }
                if (!kv[0].matches(KEY_REGEX)) {
                    throw new HTTPResponseHeadersFormatException("Invalid field name: " + kv[0]);
                }
                this.fields.put(kv[0], kv[1].trim());
            }
        }

        public void add(String name, String value) throws HTTPResponseHeadersFormatException {
            if (!modifiable) return;
            if (!name.matches(KEY_REGEX)) {
                throw new HTTPResponseHeadersFormatException("Invalid field name: " + name);
            }
            fields.put(name, value);
        }

        public String get(String name) {
            return fields.get(name);
        }

        public HashMap<String, String> getHeaders() {
            return new HashMap<>(fields);
        }

        public boolean contains(String name) {
            return fields.containsKey(name);
        }

        public byte[] getBytes() {
            StringJoiner joiner = new StringJoiner("\r\n", "", "\r\n");
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                joiner.add(entry.getKey() + ": " + entry.getValue());
            }
            return EncodingUtil.encodeText(joiner.toString());
        }
    }

    public static class HTTPResponseBody {
        private byte[] body;
        private boolean modifiable;

        public HTTPResponseBody() {
            this.body = new byte[0];
            modifiable = true;
        }

        public HTTPResponseBody(String body) {
            this.body = EncodingUtil.encodeBinary(body);
            modifiable = false;
        }

        public void setBody(byte[] body) {
            if (!modifiable) return;
            this.body = body;
        }

        public byte[] getBody() {
            return body;
        }

        public byte[] getBytes() {
            return body;
        }
    }

    public HTTPStatusLine getStatusLine() {
        return statusLine;
    }

    public HTTPResponseHeaders getHeaders() {
        return headers;
    }

    public HTTPResponseBody getBody() {
        return body;
    }

    public HTTPResponse() {
        statusLine = new HTTPStatusLine();
        headers = new HTTPResponseHeaders();
        body = new HTTPResponseBody();
    }

    public HTTPResponse(String message) throws HTTPResponseFormatException {
        parse(message);
    }

    private void parse(String message) throws HTTPResponseFormatException {
        String[] parts1 = message.split("\r\n", 2);
        if (parts1.length != 2) {
            throw new HTTPResponseFormatException("Lack necessary parts");
        }
        statusLine = new HTTPStatusLine(parts1[0]);

        String[] parts2 = parts1[1].split("(?<=\r\n)\r\n");
        headers = new HTTPResponseHeaders(parts2[0]);

        int contentLength = Integer.parseInt(headers.get("Content-Length"));
        body = new HTTPResponseBody();
        if (contentLength > 0) {
            if (parts2.length != 2 || parts2[1].length() < contentLength) {
                throw new HTTPResponseFormatException("Lack body");
            }
            body = new HTTPResponseBody(parts2[1].substring(0, contentLength));
        }
    }

    public byte[] getBytes() {
        return EncodingUtil.encodeBinary(
                String.join("\r\n",
                        EncodingUtil.decodeBinary(statusLine.getBytes()),
                        EncodingUtil.decodeBinary(headers.getBytes()),
                        EncodingUtil.decodeBinary(body.getBytes())) );
    }
}
