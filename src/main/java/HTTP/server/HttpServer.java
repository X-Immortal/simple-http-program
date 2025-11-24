package HTTP.server;// HttpServer.java

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;
    private static final Map<String, User> users = new ConcurrentHashMap<>();
    private static final Map<String, String> sessions = new ConcurrentHashMap<>();

    // 支持的MIME类型
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("png", "image/png");
    }

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("HTTP Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 OutputStream out = clientSocket.getOutputStream()) {

                // 设置Socket读取超时（防止连接永远挂起）
                clientSocket.setSoTimeout(30000); // 30秒超时

                // 长连接处理循环
                while (!clientSocket.isClosed()) {
                    // 读取请求
                    String requestLine = in.readLine();
                    if (requestLine == null) return;

                    if (requestLine.isEmpty()) {
                        Thread.sleep(10);
                        continue;
                    }

                    System.out.println("Request: " + requestLine);

                    // 解析请求
                    String[] parts = requestLine.split(" ");
                    if (parts.length < 3) {
                        sendErrorResponse(out, 400, "Bad Request");
                        return;
                    }

                    String method = parts[0];
                    String path = parts[1];

                    // 读取请求头
                    Map<String, String> headers = new HashMap<>();
                    String line;
                    while ((line = in.readLine()) != null && !line.isEmpty()) {
                        int colonIndex = line.indexOf(":");
                        if (colonIndex > 0) {
                            String key = line.substring(0, colonIndex).trim();
                            String value = line.substring(colonIndex + 1).trim();
                            headers.put(key, value);
                        }
                    }

                    boolean keepAlive = "keep-alive".equalsIgnoreCase(headers.getOrDefault("Connection", ""));

                    // 读取POST请求体
                    String body = "";
                    if ("POST".equalsIgnoreCase(method)) {
                        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
                        if (contentLength > 0) {
                            char[] bodyChars = new char[contentLength];
                            in.read(bodyChars);
                            body = new String(bodyChars);
                        }
                    }

                    // 处理请求
                    handleRequest(method, path, headers, body, out, keepAlive);

                    // 如果不是长连接，退出循环
                    if (!keepAlive) {
                        break;
                    }

                    // 短暂休眠，防止CPU过度占用
                    Thread.sleep(10);
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Connection timeout, closing socket");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleRequest(String method, String path, Map<String, String> headers,
                                   String body, OutputStream out, boolean keepAlive) throws IOException {

            // 路由处理
            switch (path) {
                case "/register":
                    if ("POST".equalsIgnoreCase(method)) {
                        handleRegister(body, out, keepAlive);
                    } else {
                        sendErrorResponse(out, 405, "Method Not Allowed", keepAlive);
                    }
                    break;
                case "/login":
                    if ("POST".equalsIgnoreCase(method)) {
                        handleLogin(body, out, keepAlive);
                    } else {
                        sendErrorResponse(out, 405, "Method Not Allowed", keepAlive);
                    }
                    break;
                case "/old-page":
                    sendRedirectResponse(out, 301, "/", keepAlive);
                    break;
                case "/temp-redirect":
                    sendRedirectResponse(out, 302, "/", keepAlive);
                    break;
                case "/error":  // 添加触发错误的路由
                    simulateError(out, keepAlive);
                    break;

                default:
                    serveFile(path, headers,out, keepAlive);
                    break;
            }
        }

        private void handleRegister(String body, OutputStream out, boolean keepAlive) throws IOException {
            try {
                Map<String, String> params = parseFormData(body);
                String username = params.get("username");
                String password = params.get("password");

                if (username == null || password == null) {
                    sendErrorResponse(out, 400, "Missing username or password", keepAlive);
                    return;
                }

                if (users.containsKey(username)) {
                    sendErrorResponse(out, 409, "User already exists", keepAlive);
                    return;
                }

                users.put(username, new User(username, password));
                sendJsonResponse(out, 200, "{\"message\": \"Registration successful\"}", keepAlive);

            } catch (Exception e) {
                sendErrorResponse(out, 500, "Internal Server Error", keepAlive);
            }
        }

        private void handleLogin(String body, OutputStream out, boolean keepAlive) throws IOException {
            try {
                Map<String, String> params = parseFormData(body);
                String username = params.get("username");
                String password = params.get("password");

                if (username == null || password == null) {
                    sendErrorResponse(out, 400, "Missing username or password", keepAlive);
                    return;
                }

                User user = users.get(username);
                if (user == null || !user.password.equals(password)) {
                    sendErrorResponse(out, 401, "Invalid credentials", keepAlive);
                    return;
                }

                // 创建会话
                String sessionId = UUID.randomUUID().toString();
                sessions.put(sessionId, username);

                Map<String, String> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("sessionId", sessionId);

                sendJsonResponse(out, 200, toJson(response), keepAlive);

            } catch (Exception e) {
                sendErrorResponse(out, 500, "Internal Server Error", keepAlive);
            }
        }

        private void serveFile(String path, Map<String, String> headers,OutputStream out, boolean keepAlive) throws IOException {
            if ("/".equals(path)) {
                path = "/index.html";
            }

            String filename = "./www" + path;
            File file = new File(filename);

            if (!file.exists() || file.isDirectory()) {
                sendErrorResponse(out, 404, "Not Found", keepAlive);
                return;
            }

            long lastModified = file.lastModified();
            Date lastModifiedDate = new Date(lastModified);

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            String lastModifiedStr = dateFormat.format(lastModifiedDate);

            String eTag = "\"" + Integer.toHexString((path + lastModified).hashCode()) + "\"";

            String ifModifiedSince = headers.get("If-Modified-Since");
            String ifNoneMatch = headers.get("If-None-Match");

            if ((ifModifiedSince != null && ifModifiedSince.equals(lastModifiedStr)) ||
                    (ifNoneMatch != null && ifNoneMatch.equals(eTag))) {
                sendNotModifiedResponse(out, lastModifiedStr, eTag, keepAlive);
                return;
            }

            // 获取MIME类型
            String extension = getFileExtension(filename);
            String mimeType = MIME_TYPES.getOrDefault(extension, "application/octet-stream");

            // 读取文件内容
            byte[] fileContent = readFileContent(file);

            // 发送响应
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + mimeType + "\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    (keepAlive ? "Connection: keep-alive\r\n" : "Connection: close\r\n") +
                    "\r\n";

            out.write(response.getBytes());
            out.write(fileContent);
            out.flush();
        }

        private void sendNotModifiedResponse(OutputStream out, String lastModified, String eTag, boolean keepAlive) throws IOException {
            String response = "HTTP/1.1 304 Not Modified\r\n" +
                    "Last-Modified: " + lastModified + "\r\n" +
                    "ETag: " + eTag + "\r\n" +
                    (keepAlive ? "Connection: keep-alive\r\n" : "Connection: close\r\n") +
                    "\r\n";

            out.write(response.getBytes());
            out.flush();
        }

        private void sendJsonResponse(OutputStream out, int statusCode, String json, boolean keepAlive) throws IOException {
            String statusText = getStatusText(statusCode);
            String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + json.length() + "\r\n" +
                    (keepAlive ? "Connection: keep-alive\r\n" : "Connection: close\r\n") +
                    "\r\n" + json;

            out.write(response.getBytes());
            out.flush();
        }

        private void sendRedirectResponse(OutputStream out, int statusCode, String location, boolean keepAlive) throws IOException {
            String statusText = getStatusText(statusCode);
            String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                    "Location: " + location + "\r\n" +
                    "Content-Length: 0\r\n" +
                    (keepAlive ? "Connection: keep-alive\r\n" : "Connection: close\r\n") +
                    "\r\n";

            out.write(response.getBytes());
            out.flush();
        }

        private void sendErrorResponse(OutputStream out, int statusCode, String message, boolean keepAlive) throws IOException {
            String statusText = getStatusText(statusCode);
            String json = "{\"error\": \"" + message + "\"}";

            String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + json.length() + "\r\n" +
                    (keepAlive ? "Connection: keep-alive\r\n" : "Connection: close\r\n") +
                    "\r\n" + json;

            out.write(response.getBytes());
            out.flush();
        }

        private void sendErrorResponse(OutputStream out, int statusCode, String message) throws IOException {
            sendErrorResponse(out, statusCode, message, false);
        }

        private void simulateError(OutputStream out, boolean keepAlive) {
            try {
                throw new RuntimeException("Simulated internal server error for testing");
            } catch (Exception e) {
                // 异常会被捕获，然后返回500错误
                try {
                    sendErrorResponse(out, 500, "Internal Server Error: " + e.getMessage(), keepAlive);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

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

        private byte[] readFileContent(File file) throws IOException {
            FileInputStream fis = new FileInputStream(file);
            byte[] content = new byte[(int) file.length()];
            fis.read(content);
            fis.close();
            return content;
        }

        private String getStatusText(int statusCode) {
            switch (statusCode) {
                case 200: return "OK";
                case 301: return "Moved Permanently";
                case 302: return "Found";
                case 304: return "Not Modified";
                case 400: return "Bad Request";
                case 401: return "Unauthorized";
                case 404: return "Not Found";
                case 405: return "Method Not Allowed";
                case 409: return "Conflict";
                case 500: return "Internal Server Error";
                default: return "Unknown";
            }
        }
    }

    static class User {
        String username;
        String password;

        User(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}