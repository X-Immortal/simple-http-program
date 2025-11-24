package HTTP.client;

import TCP.TCPClient;
import TCP.TCPServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class HttpClient extends TCPClient {
    private static final int MAX_REDIRECTS = 5;
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String CRLF = "\r\n";
    private static final String HOST_HEADER = "Host: %s\r\n";
    private String path;

    public HttpClient(String host, int port, String path) {
        super(host, port);
        this.path = path;
    }

    public HttpClient(URL url) {
        super(url);
        path = url.getPath();
    }

    public void connect() {
        if (!this.isReady()) {
            this.start();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("GET / ").append(HTTP_VERSION).append(CRLF);
        sb.append(String.format(HOST_HEADER, host));
        try {
            sendMessage(sb.toString().getBytes());
        } catch (IOException e) {
            System.out.println("Failed to send connection request");
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java HttpClient <URL> [--method GET|POST] [--header <header>] [--data <data>] [--keep-alive]");
            return;
        }

        try {
            String url = args[0];
            String method = "GET";
            String data = null;
            Map<String, String> customHeaders = new HashMap<>();
            boolean keepAlive = false;
            // 解析命令行参数
            for (int i = 1; i < args.length; i++) {
                if ("--method".equals(args[i]) && i + 1 < args.length) {
                    method = args[++i];
                } else if ("--data".equals(args[i]) && i + 1 < args.length) {
                    data = args[++i];
                }  else if ("--header".equals(args[i]) && i + 1 < args.length) {
                    String[] headerParts = args[++i].split(":", 2);
                    if (headerParts.length == 2) {
                        customHeaders.put(headerParts[0].trim(), headerParts[1].trim());
                    }
                } else if ("--keep-alive".equals(args[i])) {
                    keepAlive = true;
                }
            }

            HttpResponse response = sendRequest(url, method, data, customHeaders,0, keepAlive);
            printResponse(response);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static HttpResponse sendRequest(String urlStr, String method, String data, Map<String, String> customHeaders,int redirectCount, boolean keepAlive) throws IOException {
        if (redirectCount > MAX_REDIRECTS) {
            throw new IOException("Too many redirects");
        }

        URL url = new URL(urlStr);
        String host = url.getHost();
        int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
        String path = url.getPath().isEmpty() ? "/" : url.getPath();

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 发送请求
            out.print(method + " " + path + " HTTP/1.1\r\n");
            out.print("Host: " + host + "\r\n");
            if (!customHeaders.isEmpty()) {
                for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                    out.print(entry.getKey() + ": " + entry.getValue() + "\r\n");
                }
            }
            out.print("Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n");

            if (data != null) {
                out.print("Content-Type: application/x-www-form-urlencoded\r\n");
                out.print("Content-Length: " + data.length() + "\r\n");
            }
            out.print("\r\n");

            if (data != null) {
                out.print(data);
            }
            out.flush();

            // 读取响应
            return readResponse(in, urlStr, method, data,  customHeaders, redirectCount, keepAlive);
        }
    }

    private static HttpResponse readResponse(BufferedReader in, String originalUrl,
                                             String originalMethod, String originalData, Map<String, String> customHeaders,
                                             int redirectCount, boolean keepAlive) throws IOException {
        String statusLine = in.readLine();
        if (statusLine == null) {
            throw new IOException("No response from server");
        }

        // 解析状态行
        String[] statusParts = statusLine.split(" ", 3);
        int statusCode = Integer.parseInt(statusParts[1]);
        String statusMessage = statusParts.length > 2 ? statusParts[2] : "";

        // 读取响应头
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

        // 读取响应体
        StringBuilder body = new StringBuilder();
        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            in.read(bodyChars);
            body.append(bodyChars);
        } else {
            // 如果没有Content-Length，读取直到连接关闭
            while (in.ready()) {
                body.append((char) in.read());
            }
        }

        HttpResponse response = new HttpResponse(statusCode, statusMessage, headers, body.toString());

        // 处理重定向
        if (statusCode == 301 || statusCode == 302) {
            String location = headers.get("Location");
            if (location != null) {
                System.out.println("Redirecting to: " + location);
                return sendRequest("http://localhost:8080" + location, originalMethod, originalData, customHeaders,redirectCount + 1, keepAlive);
            }
        }

        // 处理304 Not Modified
        if (statusCode == 304) {
            System.out.println("Resource not modified - using cached version");
        }

        return response;
    }

    private static void printResponse(HttpResponse response) {
        System.out.println("HTTP/1.1 " + response.statusCode + " " + response.statusMessage);
        for (Map.Entry<String, String> header : response.headers.entrySet()) {
            System.out.println(header.getKey() + ": " + header.getValue());
        }
        System.out.println();
        System.out.println(response.body);
    }

    static class HttpResponse {
        int statusCode;
        String statusMessage;
        Map<String, String> headers;
        String body;

        HttpResponse(int statusCode, String statusMessage, Map<String, String> headers, String body) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
            this.headers = headers;
            this.body = body;
        }
    }
}