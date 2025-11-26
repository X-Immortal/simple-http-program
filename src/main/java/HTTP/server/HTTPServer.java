package HTTP.server;

import HTTP.exception.HTTPMethodNotAllowedException;
import HTTP.exception.HTTPRequestFormatException;
import HTTP.exception.HTTPResponseFormatException;
import HTTP.message.HTTPRequest;
import HTTP.message.HTTPResponse;
import HTTP.rule.HTTPEncodingRule;
import HTTP.rule.HTTPVersion;
import TCP.TCPServer;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class HTTPServer extends TCPServer {
    private static final String PATH_SEPARATOR = FileSystems.getDefault().getSeparator();
    private static final Map<String, String> sessions = new ConcurrentHashMap<>();
    private static final String ROOT_PATH = "." + PATH_SEPARATOR + "root" + PATH_SEPARATOR;
    private static final String DEFAULT_FILE_PATH = ROOT_PATH + "welcome.txt";
    private static final String MSG_BODY_PATH = ROOT_PATH + "msgbody" + PATH_SEPARATOR;
    private static final String SERVER_NAME = "Simple HTTP Server";
    private final HashMap<String, Function<HTTPRequest, HTTPResponse>> routerMap = new HashMap<>();

    // 支持的MIME类型
    private static final Map<String, String> typeMap = new HashMap<>();

    static {
        typeMap.put("html", "text/html");
        typeMap.put("txt", "text/plain");
        typeMap.put("json", "application/json");
        typeMap.put("jpg", "image/jpeg");
        typeMap.put("png", "image/png");
    }

    {
        routerMap.put("/", this::handleDefault);
        routerMap.put("/register", this::handleRegister);
        routerMap.put("/login", this::handleLogin);
        routerMap.put("/document", this::handleDocument);
    }

    public HTTPServer(int port) {
        super(port);
    }

    public void run() {
        run(bytes -> {
            if (bytes == null || bytes.length == 0) return null;
            showReceivedMessage.accept(bytes);
            return handleRequest(bytes);
        });
    }

    private byte[] handleRequest(byte[] message) {
        String messageStr = HTTPEncodingRule.decodeBinary(message);
        HTTPRequest request;
        try {
            request = new HTTPRequest(messageStr);
        } catch (HTTPRequestFormatException e) {
            return handleBadRequest().getBytes();
        } catch (HTTPMethodNotAllowedException e) {
            return handleMethodNotAllowed(e.getMessage()).getBytes();
        }
        String path = request.getRequestLine().getPath();
        if (!routerMap.containsKey(request.getRequestLine().getPath())) {
            return handleNotFound().getBytes();
        }
        return routerMap.get(request.getRequestLine().getPath()).apply(request).getBytes();
    }

    private String getRouter(String path) {
        // TODO
        return null;
    }

    private byte[] readFile(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path)) {
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[1024];
            int n;
            while ((n = fis.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, HTTPEncodingRule.BINARY_CHARSET));
            }
            return HTTPEncodingRule.encodeBinary(sb.toString());
        }
    }

    private HTTPResponse handleDefault(HTTPRequest request) {
        HTTPResponse response = new HTTPResponse();
        try {
            byte[] content = readFile(DEFAULT_FILE_PATH);

            response.getStatusLine().setVersion(request.getRequestLine().getVersion());
            response.getStatusLine().setStatusCode(200);
            response.getHeaders().add("Content-Type", typeMap.get("txt"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length));
            response.getHeaders().add("Server", SERVER_NAME);
            response.getBody().setBody(content);
            return response;
        } catch (FileNotFoundException e) {
            return handleNotFound();
        } catch (IOException | HTTPResponseFormatException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleRegister(HTTPRequest request) {
        // TODO
        return null;
    }

    private HTTPResponse handleLogin(HTTPRequest request) {
        // TODO
        return null;
    }

    private String listFiles(String path) {
        StringBuilder sb = new StringBuilder();
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) return "";
        for (File f : files) {
            sb.append(f.getName()).append("\n");
        }
        return sb.toString();
    }

    private HTTPResponse handleDocument(HTTPRequest request) {
        if (!request.getRequestLine().getMethod().equals("GET")) {
            return handleMethodNotAllowed("Only GET method is allowed for this resource");
        }

        HTTPResponse response = new HTTPResponse();
        String path = request.getRequestLine().getPath();

        try {
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(200);
            if (path.equals("/document")) {
                String content = listFiles(ROOT_PATH + "document");

                response.getHeaders().add("Content-Type", typeMap.get("txt"));
                response.getHeaders().add("Content-Length", String.valueOf(content.length()));
                response.getBody().setBody(HTTPEncodingRule.encodeText(content));
            } else {
                byte[] content = readFile(ROOT_PATH + path);

                String extension = getFileExtension(path);
                if (extension.isEmpty()) {
                    return handleInternalServerError();
                }

                response.getHeaders().add("Content-Type", typeMap.get(extension));
                response.getHeaders().add("Content-Length", String.valueOf(content.length));
                response.getBody().setBody(content);
            }

            return response;
        } catch (FileNotFoundException e) {
            return handleNotFound();
        } catch (HTTPResponseFormatException | IOException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleBadRequest() {
        HTTPResponse response = new HTTPResponse();
        try {
            byte[] content = readFile(MSG_BODY_PATH + "400.txt");

            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(400);
            response.getHeaders().add("Content-Type", typeMap.get("txt"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length));
            response.getBody().setBody(content);
            return response;
        } catch (HTTPResponseFormatException | IOException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleNotFound() {
        HTTPResponse response = new HTTPResponse();
        try {
            byte[] content = readFile(MSG_BODY_PATH + "404.txt");

            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(404);
            response.getHeaders().add("Content-Type", typeMap.get("txt"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length));
            response.getBody().setBody(content);
            return response;
        } catch (HTTPResponseFormatException | IOException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleInternalServerError() {
        HTTPResponse response = new HTTPResponse();
        try {
            byte[] content = readFile(MSG_BODY_PATH + "500.txt");

            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(500);
            response.getHeaders().add("Content-Type", typeMap.get("txt"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length));
            response.getBody().setBody(content);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private HTTPResponse handleMethodNotAllowed(String detail) {
        HTTPResponse response = new HTTPResponse();

        try {
            String content = HTTPEncodingRule.decodeText(readFile(MSG_BODY_PATH + "405.txt"));
            content = String.format(content, detail);

            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(405);
            response.getHeaders().add("Content-Type", typeMap.get("txt"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length()));
            response.getBody().setBody(HTTPEncodingRule.encodeText(content));
            return response;
        } catch (IOException | IllegalFormatException | HTTPResponseFormatException e) {
            return handleInternalServerError();
        }
    }

    private String getFileTimestamp(String path) throws FileNotFoundException {
        File file = new File(path);

        if (!file.exists() || file.isDirectory()) {
            throw new FileNotFoundException(path);
        }

        Date lastModified = new Date(file.lastModified());

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(lastModified);
    }

    private HTTPResponse handleNotModified(HTTPRequest request) {
        HTTPResponse response = new HTTPResponse();

        try {
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(304);
            response.getHeaders().add("Last-Modified", request.getHeaders().get("If-Modified-Since"));
            return response;
        } catch (HTTPResponseFormatException e) {
            return handleInternalServerError();
        }
    }

//    private void handleRequest(String method, String path, Map<String, String> headers,
//                               String body, OutputStream out, boolean keepAlive) throws IOException {
//
//        // 路由处理
//        switch (path) {
//            case "/register":
//                if ("POST".equalsIgnoreCase(method)) {
//                    handleRegister(body, out, keepAlive);
//                } else {
//                    sendErrorResponse(out, 405, "Method Not Allowed", keepAlive);
//                }
//                break;
//            case "/login":
//                if ("POST".equalsIgnoreCase(method)) {
//                    handleLogin(body, out, keepAlive);
//                } else {
//                    sendErrorResponse(out, 405, "Method Not Allowed", keepAlive);
//                }
//                break;
//            case "/old-page":
//                sendRedirectResponse(out, 301, "/", keepAlive);
//                break;
//            case "/temp-redirect":
//                sendRedirectResponse(out, 302, "/", keepAlive);
//                break;
//            case "/error":  // 添加触发错误的路由
//                simulateError(out, keepAlive);
//                break;
//
//            default:
//                serveFile(path, headers,out, keepAlive);
//                break;
//        }
//    }



//    private void handleRegister(String body, OutputStream out, boolean keepAlive) throws IOException {
//        try {
//            Map<String, String> params = parseFormData(body);
//            String username = params.get("username");
//            String password = params.get("password");
//
//            if (username == null || password == null) {
//                sendErrorResponse(out, 400, "Missing username or password", keepAlive);
//                return;
//            }
//
//            if (users.containsKey(username)) {
//                sendErrorResponse(out, 409, "User already exists", keepAlive);
//                return;
//            }
//
//            users.put(username, new User(username, password));
//            sendJsonResponse(out, 200, "{\"message\": \"Registration successful\"}", keepAlive);
//
//        } catch (Exception e) {
//            sendErrorResponse(out, 500, "Internal Server Error", keepAlive);
//        }
//    }

//    private void handleLogin(String body, OutputStream out, boolean keepAlive) throws IOException {
//        try {
//            Map<String, String> params = parseFormData(body);
//            String username = params.get("username");
//            String password = params.get("password");
//
//            if (username == null || password == null) {
//                sendErrorResponse(out, 400, "Missing username or password", keepAlive);
//                return;
//            }
//
//            User user = users.get(username);
//            if (user == null || !user.password.equals(password)) {
//                sendErrorResponse(out, 401, "Invalid credentials", keepAlive);
//                return;
//            }
//
//            // 创建会话
//            String sessionId = UUID.randomUUID().toString();
//            sessions.put(sessionId, username);
//
//            Map<String, String> response = new HashMap<>();
//            response.put("message", "Login successful");
//            response.put("sessionId", sessionId);
//
//            sendJsonResponse(out, 200, toJson(response), keepAlive);
//
//        } catch (Exception e) {
//            sendErrorResponse(out, 500, "Internal Server Error", keepAlive);
//        }
//    }

//    private void serveFile(String path, Map<String, String> headers,OutputStream out, boolean keepAlive) throws IOException {
//        String eTag = "\"" + Integer.toHexString((path + lastModified).hashCode()) + "\"";
//
//        String ifModifiedSince = headers.get("If-Modified-Since");
//        String ifNoneMatch = headers.get("If-None-Match");
//
//        if ((ifModifiedSince != null && ifModifiedSince.equals(lastModifiedStr)) ||
//                (ifNoneMatch != null && ifNoneMatch.equals(eTag))) {
//            sendNotModifiedResponse(out, lastModifiedStr, eTag, keepAlive);
//            return;
//        }
//
//        // 获取MIME类型
//        String extension = getFileExtension(filename);
//        String mimeType = typeMap.getOrDefault(extension, "application/octet-stream");
//
//        // 读取文件内容
//        byte[] fileContent = readFileContent(file);
//
//        // 发送响应
//        String response = "HTTP/1.1 200 OK\r\n" +
//                "Content-Type: " + mimeType + "\r\n" +
//                "Content-Length: " + fileContent.length + "\r\n" +
//                (keepAlive ? "Connection: keep-alive\r\n" : "Connection: close\r\n") +
//                "\r\n";
//
//        out.write(response.getBytes());
//        out.write(fileContent);
//        out.flush();
//    }

//    private void sendNotModifiedResponse(OutputStream out, String lastModified, String eTag, boolean keepAlive) throws IOException {
//        String response = "HTTP/1.1 304 Not Modified\r\n" +
//                "Last-Modified: " + lastModified + "\r\n" +
//                "ETag: " + eTag + "\r\n" +
//                (keepAlive ? "Connection: keep-alive\r\n" : "Connection: close\r\n") +
//                "\r\n";
//
//        out.write(response.getBytes());
//        out.flush();
//    }

//    private void sendJsonResponse(OutputStream out, int statusCode, String json, boolean keepAlive) throws IOException {
//        String statusText = getStatusText(statusCode);
//        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
//                "Content-Type: application/json\r\n" +
//                "Content-Length: " + json.length() + "\r\n" +
//                (keepAlive ? "Connection: keep-alive\r\n" : "Connection: close\r\n") +
//                "\r\n" + json;
//
//        out.write(response.getBytes());
//        out.flush();
//    }

//    private void sendRedirectResponse(OutputStream out, int statusCode, String location, boolean keepAlive) throws IOException {
//        String statusText = getStatusText(statusCode);
//        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
//                "Location: " + location + "\r\n" +
//                "Content-Length: 0\r\n" +
//                (keepAlive ? "Connection: keep-alive\r\n" : "Connection: close\r\n") +
//                "\r\n";
//
//        out.write(response.getBytes());
//        out.flush();
//    }

//    private void simulateError(OutputStream out, boolean keepAlive) {
//        try {
//            throw new RuntimeException("Simulated internal server error for testing");
//        } catch (Exception e) {
//            // 异常会被捕获，然后返回500错误
//            try {
//                sendErrorResponse(out, 500, "Internal Server Error: " + e.getMessage(), keepAlive);
//            } catch (IOException ioException) {
//                ioException.printStackTrace();
//            }
//        }
//    }
    // 辅助方法
    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        if (formData != null && !formData.isEmpty()) {
            String[] pairs = formData.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], URLDecoder.decode(keyValue[1]));
                }
            }
        }
        return params;
    }

    private String toJson(Map<String, String> map) {
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (json.length() > 1) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
        }
        json.append("}");
        return json.toString();
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return dotIndex == -1 ? "" : filename.substring(dotIndex + 1);
    }
}