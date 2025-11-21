package com.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleHttpServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Map<String, User> userDatabase;
    private boolean isRunning;
    private int port;

    // 用户类
    static class User {
        String username;
        String password;
        String email;

        User(String username, String password, String email) {
            this.username = username;
            this.password = password;
            this.email = email;
        }
    }

    public SimpleHttpServer(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(10);
        this.userDatabase = new ConcurrentHashMap<>();
        this.isRunning = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("HTTP Server started on port " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            isRunning = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
        threadPool.shutdown();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 客户端请求处理类
    class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                handleRequest(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleRequest(Socket socket) throws IOException {
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();

            // 解析HTTP请求
            HttpRequest request = parseRequest(input);
            HttpResponse response = new HttpResponse();

            // 处理请求
            processRequest(request, response);

            // 发送响应
            sendResponse(output, response);

            // 如果是短连接，关闭socket
            if (!"keep-alive".equalsIgnoreCase(request.getHeader("Connection"))) {
                socket.close();
            }
        }

        private HttpRequest parseRequest(InputStream input) throws IOException {
            // TODO: 实现HTTP请求解析
            return new HttpRequest();
        }

        private void processRequest(HttpRequest request, HttpResponse response) {
            String method = request.getMethod();
            String path = request.getPath();

            try {
                // 路由处理
                if ("GET".equals(method)) {
                    handleGetRequest(path, request, response);
                } else if ("POST".equals(method)) {
                    handlePostRequest(path, request, response);
                } else {
                    response.setStatusCode(405);
                    response.setStatusMessage("Method Not Allowed");
                }
            } catch (Exception e) {
                response.setStatusCode(500);
                response.setStatusMessage("Internal Server Error");
            }
        }

        private void handleGetRequest(String path, HttpRequest request, HttpResponse response) {
            // TODO: 实现GET请求处理
            if ("/".equals(path)) {
                response.setStatusCode(200);
                response.setContentType("text/html");
                response.setBody("<html><body><h1>Welcome</h1></body></html>");
            } else {
                response.setStatusCode(404);
                response.setStatusMessage("Not Found");
            }
        }

        private void handlePostRequest(String path, HttpRequest request, HttpResponse response) {
            // TODO: 实现POST请求处理 - 注册和登录
            if ("/register".equals(path)) {
                handleRegister(request, response);
            } else if ("/login".equals(path)) {
                handleLogin(request, response);
            } else {
                response.setStatusCode(404);
                response.setStatusMessage("Not Found");
            }
        }

        private void handleRegister(HttpRequest request, HttpResponse response) {
            // TODO: 实现用户注册逻辑
            response.setStatusCode(200);
            response.setContentType("application/json");
            response.setBody("{\"status\":\"success\",\"message\":\"Registration successful\"}");
        }

        private void handleLogin(HttpRequest request, HttpResponse response) {
            // TODO: 实现用户登录逻辑
            response.setStatusCode(200);
            response.setContentType("application/json");
            response.setBody("{\"status\":\"success\",\"message\":\"Login successful\"}");
        }

        private void sendResponse(OutputStream output, HttpResponse response) throws IOException {
            // TODO: 实现HTTP响应发送
            PrintWriter writer = new PrintWriter(output);
            writer.println("HTTP/1.1 " + response.getStatusCode() + " " + response.getStatusMessage());
            writer.println("Content-Type: " + response.getContentType());
            writer.println("Content-Length: " + response.getBody().length());
            writer.println("Connection: keep-alive");
            writer.println();
            writer.println(response.getBody());
            writer.flush();
        }
    }

    public static void main(String[] args) {
        SimpleHttpServer server = new SimpleHttpServer(8080);
        server.start();
    }
}