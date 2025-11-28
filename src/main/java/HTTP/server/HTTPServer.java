package HTTP.server;

import HTTP.exception.HTTPMethodNotAllowedException;
import HTTP.exception.HTTPRequestFormatException;
import HTTP.exception.HTTPResponseFormatException;
import HTTP.message.HTTPRequest;
import HTTP.message.HTTPResponse;
import HTTP.rule.HTTPVersion;
import HTTP.utils.FileUtil;
import HTTP.utils.HTTPEncodingUtil;
import TCP.TCPServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
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
    private final HashMap<String, String> redirectMap = new HashMap<>();

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

        redirectMap.put("/file", "/document");
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
        String messageStr = HTTPEncodingUtil.decodeBinary(message);
        HTTPRequest request;
        try {
            request = new HTTPRequest(messageStr);
        } catch (HTTPRequestFormatException e) {
            e.printStackTrace();
            return handleBadRequest().getBytes();
        } catch (HTTPMethodNotAllowedException e) {
            return handleMethodNotAllowed(e.getMessage()).getBytes();
        }
        String path = getRouter(request.getRequestLine().getPath());
        if (!routerMap.containsKey(path)) {
            return handleNotFound().getBytes();
        }
        return routerMap.get(path).apply(request).getBytes();
    }

    private String getRouter(String path) {
        int index = path.indexOf('/', 1);
        return index == -1 ? path : path.substring(0, index);
    }

    private HTTPResponse handleDefault(HTTPRequest request) {
        HTTPResponse response = new HTTPResponse();
        try {
            byte[] content = FileUtil.read(DEFAULT_FILE_PATH);

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
        return handleInternalServerError();
    }

    private HTTPResponse handleLogin(HTTPRequest request) {
        // TODO
        return handleInternalServerError();
    }

    private HTTPResponse handleDocument(HTTPRequest request) {
        if (!request.getRequestLine().getMethod().equals("GET")) {
            return handleMethodNotAllowed("Only GET method is allowed for this resource");
        }

        HTTPResponse response = new HTTPResponse();
        String path = request.getRequestLine().getPath();
        File file = new File(ROOT_PATH + path);
        if (!file.exists()) {
            return handleNotFound();
        }

        try {
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(200);
            if (file.isDirectory()) {
                if (!path.endsWith("/")) {
                    return handleFound(path + "/");
                }
                String content = FileUtil.listFiles(ROOT_PATH + path);

                response.getHeaders().add("Content-Type", typeMap.get("txt"));
                response.getHeaders().add("Content-Length", String.valueOf(content.length()));
                response.getBody().setBody(HTTPEncodingUtil.encodeText(content));
            } else {
                String timestamp = FileUtil.getTimestamp(ROOT_PATH + path);

                if (request.getHeaders().contains("If-Modified-Since") &&
                        timestamp.equals(request.getHeaders().get("If-Modified-Since"))) {
                    return handleNotModified(request);
                }

                byte[] content = FileUtil.read(ROOT_PATH + path);

                String extension = FileUtil.getExtension(path);
                if (extension.isEmpty()) {
                    return handleInternalServerError();
                }

                response.getHeaders().add("Content-Type", typeMap.get(extension));
                response.getHeaders().add("Content-Length", String.valueOf(content.length));
                response.getHeaders().add("Last-Modified", timestamp);
                response.getHeaders().add("Cache-Control", "no-cache");
                response.getBody().setBody(content);
            }
            return response;
        } catch (HTTPResponseFormatException | IOException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleBadRequest() {
        HTTPResponse response = new HTTPResponse();
        try {
            byte[] content = FileUtil.read(MSG_BODY_PATH + "400.txt");

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
            byte[] content = FileUtil.read(MSG_BODY_PATH + "404.txt");

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
            byte[] content = FileUtil.read(MSG_BODY_PATH + "500.txt");

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
            String content = HTTPEncodingUtil.decodeText(FileUtil.read(MSG_BODY_PATH + "405.txt"));
            content = String.format(content, detail);

            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(405);
            response.getHeaders().add("Content-Type", typeMap.get("txt"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length()));
            response.getBody().setBody(HTTPEncodingUtil.encodeText(content));
            return response;
        } catch (IOException | IllegalFormatException | HTTPResponseFormatException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleMovedPermanently(String path) {
        // TODO
        HTTPResponse response = new HTTPResponse();

        try {
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(301);
            response.getHeaders().add("Location", path);
            response.getHeaders().add("Content-Length", "0");
            return response;
        } catch (HTTPResponseFormatException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleFound(String location) {
        HTTPResponse response = new HTTPResponse();

        try {
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(302);
            response.getHeaders().add("Location", location);
            response.getHeaders().add("Content-Length", "0");
            return response;
        } catch (HTTPResponseFormatException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleNotModified(HTTPRequest request) {
        HTTPResponse response = new HTTPResponse();

        try {
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(304);
            response.getHeaders().add("Last-Modified", request.getHeaders().get("If-Modified-Since"));
            response.getHeaders().add("Cache-Control", "no-cache");
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
//
//            case "/error":  // 添加触发错误的路由
//                simulateError(out, keepAlive);
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

}