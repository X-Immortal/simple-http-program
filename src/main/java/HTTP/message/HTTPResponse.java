package HTTP.message;

import HTTP.exception.*;
import HTTP.rule.HTTPEncodingRule;
import HTTP.rule.HTTPVersion;

import java.util.*;

public class HTTPResponse {
    private HTTPStatusLine statusLine;
    private HTTPResponseHeaders headers;
    private HTTPResponseBody body;

    public static class HTTPStatusLine {
        private static final HashMap<Integer, String> statusCodes = new HashMap<>();

        private String version;
        private int statusCode;
        private String statusMessage;

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
        }

        public HTTPStatusLine(String statusLine) throws HTTPStatusLineFormatException {
            statusLine = HTTPEncodingRule.binaryToText(statusLine);
            String[] parts = statusLine.split(" ");
            if (parts.length != 3) {
                throw new HTTPStatusLineFormatException("Lack necessary parts");
            }

            setVersion(parts[0]);
            setStatusCode(Integer.parseInt(parts[1]));
        }

        public void setVersion(String version) throws HTTPStatusLineFormatException {
            if (!HTTPVersion.support(version)) {
                throw new HTTPStatusLineFormatException("HTTP version is not supported: " + version);
            }
            this.version = version;
        }

        public void setStatusCode(int statusCode) throws HTTPStatusLineFormatException {
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
            return HTTPEncodingRule.encodeText(String.join(" ", version, String.valueOf(statusCode), statusMessage));
        }
    }

    public static class HTTPResponseHeaders {
        private static final String KEY_REGEX = "[A-Z][a-z]*(-[A-Z][a-z]*)*";

        private final HashMap<String, String> fields = new HashMap<>();

        public HTTPResponseHeaders() {
        }

        public HTTPResponseHeaders(String headers) throws HTTPResponseHeadersFormatException {
            headers = HTTPEncodingRule.binaryToText(headers);
            parse(headers);
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
            if (!name.matches(KEY_REGEX)) {
                throw new HTTPResponseHeadersFormatException("Invalid field name: " + name);
            }
            fields.put(name, value);
        }

        public String get(String name) {
            return fields.get(name);
        }

        public HashMap<String, String> getHeaders() {
            return fields;
        }

        public byte[] getBytes() {
            StringJoiner joiner = new StringJoiner("\r\n", "", "\r\n");
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                joiner.add(entry.getKey() + ": " + entry.getValue());
            }
            return HTTPEncodingRule.encodeText(joiner.toString());
        }
    }

    public static class HTTPResponseBody {
        private byte[] body;

        public HTTPResponseBody() {
        }

        public HTTPResponseBody(String body) {
            this.body = HTTPEncodingRule.encodeBinary(body);
        }

        public void setBody(byte[] body) {
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

        if (parts2.length == 2) {
            body = new HTTPResponseBody(parts2[1]);
        }
    }

    public byte[] getBytes() {
        return HTTPEncodingRule.encodeBinary(
                String.join("\r\n",
                        HTTPEncodingRule.decodeBinary(statusLine.getBytes()),
                        HTTPEncodingRule.decodeBinary(headers.getBytes()),
                        HTTPEncodingRule.decodeBinary(body.getBytes())) );
    }
}
