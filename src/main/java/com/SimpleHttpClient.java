package com;

import com.sun.deploy.net.HttpRequest;
import com.sun.deploy.net.HttpResponse;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.Map;

public class SimpleHttpClient {
    private String host;
    private int port;
    private boolean followRedirects;

    public SimpleHttpClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.followRedirects = true;
    }

    public HttpResponse sendRequest(HttpRequest request) throws IOException {
        return sendRequest(request, 0);
    }

    private HttpResponse sendRequest(HttpRequest request, int redirectCount) throws IOException {
        if (redirectCount > 5) {
            throw new IOException("Too many redirects");
        }

        Socket socket = new Socket(host, port);

        try {
            // 发送请求
            OutputStream output = socket.getOutputStream();
            sendRequest(output, request);

            // 接收响应
            InputStream input = socket.getInputStream();
            HttpResponse response = parseResponse(input);

            // 处理重定向
            if (followRedirects && isRedirect(response.getStatusCode())) {
                String location = response.getHeader("Location");
                if (location != null) {
                    socket.close();
                    // 解析新的URL并创建新的请求
                    URL redirectUrl = new URL(location);
                    this.host = redirectUrl.getHost();
                    this.port = redirectUrl.getPort() > 0 ? redirectUrl.getPort() : 80;
                    request.setPath(redirectUrl.getPath());
                    return sendRequest(request, redirectCount + 1);
                }
            }

            return response;
        } finally {
            if (!"keep-alive".equalsIgnoreCase(request.getHeader("Connection"))) {
                socket.close();
            }
        }
    }

    private void sendRequest(OutputStream output, HttpRequest request) throws IOException {
        PrintWriter writer = new PrintWriter(output);

        // 请求行
        writer.println(request.getMethod() + " " + request.getPath() + " HTTP/1.1");

        // 头部
        writer.println("Host: " + host + ":" + port);
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            writer.println(header.getKey() + ": " + header.getValue());
        }

        // 空行分隔头部和正文
        writer.println();

        // 正文
        if (request.getBody() != null) {
            writer.print(request.getBody());
        }

        writer.flush();
    }

    private HttpResponse parseResponse(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        HttpResponse response = new HttpResponse();

        // 解析状态行
        String statusLine = reader.readLine();
        if (statusLine != null) {
            String[] parts = statusLine.split(" ", 3);
            if (parts.length >= 2) {
                response.setStatusCode(Integer.parseInt(parts[1]));
                if (parts.length == 3) {
                    response.setStatusMessage(parts[2]);
                }
            }
        }

        // 解析头部
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int separator = line.indexOf(":");
            if (separator > 0) {
                String name = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                response.setHeader(name, value);
            }
        }

        // 解析正文
        StringBuilder body = new StringBuilder();
        int contentLength = 0;
        try {
            String contentLengthHeader = response.getHeader("Content-Length");
            if (contentLengthHeader != null) {
                contentLength = Integer.parseInt(contentLengthHeader);
            }
        } catch (NumberFormatException e) {
            // 忽略格式错误
        }

        if (contentLength > 0) {
            char[] buffer = new char[contentLength];
            int bytesRead = reader.read(buffer);
            if (bytesRead > 0) {
                body.append(buffer, 0, bytesRead);
            }
        } else {
            // 如果没有Content-Length，读取所有可用数据
            while (reader.ready()) {
                body.append((char) reader.read());
            }
        }

        response.setBody(body.toString());
        return response;
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303;
    }

    public static void main(String[] args) {
        try {
            SimpleHttpClient client = new SimpleHttpClient("localhost", 8080);

            // 发送GET请求
            HttpRequest getRequest = new HttpRequest();
            getRequest.setMethod("GET");
            getRequest.setPath("/");
            getRequest.setHeader("Connection", "close");

            HttpResponse response = client.sendRequest(getRequest);
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}