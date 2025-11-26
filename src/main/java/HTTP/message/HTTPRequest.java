package HTTP.message;

import HTTP.exception.HTTPMethodNotAllowedException;
import HTTP.exception.HTTPRequestFormatException;
import HTTP.exception.HTTPRequestHeadersFormatException;
import HTTP.exception.HTTPRequestLineFormatException;
import HTTP.rule.HTTPEncodingRule;
import HTTP.rule.HTTPVersion;

import java.util.*;

public class HTTPRequest {
    private HTTPRequestLine requestLine;
    private HTTPRequestHeaders headers;
    private HTTPRequestBody body;

    public static class HTTPRequestLine {
        private static final HashSet<String> supportedMethods = new HashSet<>();
        private static final String PATH_REGEX = "(/|(/[\\w-~\\.]+)+)(\\?\\w+(=\\w*)?(&\\w+(=\\w*)?)*)?";

        private String method;
        private String path;
        private String version;

        static {
            Collections.addAll(supportedMethods, "GET", "POST");
        }

        public HTTPRequestLine() {
        }

        public HTTPRequestLine(String line) throws HTTPRequestLineFormatException, HTTPMethodNotAllowedException {
            line = HTTPEncodingRule.binaryToText(line);
            parse(line);
        }

        private void parse(String line) throws HTTPRequestLineFormatException, HTTPMethodNotAllowedException {
            String[] parts = line.split(" ");
            if (parts.length != 3) {
                throw new HTTPRequestLineFormatException("Lack necessary parts");
            }

            setMethod(parts[0]);
            setPath(parts[1]);
            setVersion(parts[2]);
        }

        private void setMethod(String method) throws HTTPMethodNotAllowedException {
            if (!supportedMethods.contains(method)) {
                throw new HTTPMethodNotAllowedException("Method is not supported: " + method);
            }
            this.method = method;
        }

        private void setPath(String path) throws HTTPRequestLineFormatException {
            if (!path.matches(PATH_REGEX)) {
                throw new HTTPRequestLineFormatException("Invalid path: " + path);
            }
            this.path = path;
        }

        private void setVersion(String version) throws HTTPRequestLineFormatException {
            if (!HTTPVersion.support(version)) {
                throw new HTTPRequestLineFormatException("Version is not supported: " + version);
            }
            this.version = version;
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public String getVersion() {
            return version;
        }

        public byte[] getBytes() {
            return HTTPEncodingRule.encodeText(String.join(" ", method, path, version));
        }
    }

    public static class HTTPRequestHeaders {
        private static final String KEY_REGEX = "[A-Z][a-z]*(-[A-Z][a-z]*)*";

        private final HashMap<String, String> fields = new HashMap<>();

        public HTTPRequestHeaders() {
        }

        public HTTPRequestHeaders(String headers) throws HTTPRequestHeadersFormatException {
            headers = HTTPEncodingRule.binaryToText(headers);
            parse(headers);
        }

        private void parse(String headers) throws HTTPRequestHeadersFormatException {
            String[] fields = headers.split("\r\n");
            for (String field : fields) {
                String[] kv = field.split(": ", 2);
                if (kv.length != 2) {
                    throw new HTTPRequestHeadersFormatException("Invalid field: " + field);
                }
                if (!kv[0].matches(KEY_REGEX)) {
                    throw new HTTPRequestHeadersFormatException("Invalid field name: " + kv[0]);
                }
                this.fields.put(kv[0], kv[1].trim());
            }
        }

        public void add(String name, String value) throws HTTPRequestHeadersFormatException {
            if (!name.matches(KEY_REGEX)) {
                throw new HTTPRequestHeadersFormatException("Invalid field name: " + name);
            }
            fields.put(name, value);
        }

        public String get(String name) {
            return fields.get(name);
        }

        public HashMap<String, String> getFields() {
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

    public static class HTTPRequestBody {
        private byte[] body;

        public HTTPRequestBody() {
            body = new byte[0];
        }

        public HTTPRequestBody(byte[] body) {
            setBody(body);
        }

        public HTTPRequestBody(String body) {
            setBody(HTTPEncodingRule.encodeBinary(body));
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

    public HTTPRequestLine getRequestLine() {
        return requestLine;
    }

    public HTTPRequestHeaders getHeaders() {
        return headers;
    }

    public HTTPRequestBody getBody() {
        return body;
    }

    public HTTPRequest() {
        requestLine = new HTTPRequestLine();
        headers = new HTTPRequestHeaders();
        body = new HTTPRequestBody();
    }

    public HTTPRequest(String message) throws HTTPRequestFormatException, HTTPMethodNotAllowedException {
        parse(message);
    }

    private void parse(String message) throws HTTPRequestFormatException, HTTPMethodNotAllowedException {
        String[] parts1 = message.split("\r\n", 2);
        if (parts1.length != 2) {
            throw new HTTPRequestFormatException("Lack necessary parts");
        }
        requestLine = new HTTPRequestLine(parts1[0]);

        String[] parts2 = parts1[1].split("(?<=\r\n)\r\n");
        headers = new HTTPRequestHeaders(parts2[0]);

        if (requestLine.getMethod().equals("GET") && parts2.length != 1) {
            throw new HTTPRequestFormatException("GET method cannot have body");
        } else if (requestLine.getMethod().equals("POST")) {
            if (parts2.length != 2) {
                throw new HTTPRequestFormatException("POST method must have body");
            }
            body = new HTTPRequestBody(parts2[1]);
        }
    }

    public byte[] getBytes() {
        return HTTPEncodingRule.encodeBinary(
                String.join("\r\n",
                        HTTPEncodingRule.decodeBinary(requestLine.getBytes()),
                        HTTPEncodingRule.decodeBinary(headers.getBytes()),
                        HTTPEncodingRule.decodeBinary(body.getBytes())));
    }
}
