package HTTP.server;

import HTTP.message.HTTPRequest;
import HTTP.message.HTTPResponse;
import HTTP.message.exception.HTTPMethodNotAllowedException;
import HTTP.message.exception.HTTPRequestFormatException;
import HTTP.message.exception.HTTPResponseFormatException;
import HTTP.rule.HTTPVersion;
import HTTP.rule.MIME;
import HTTP.rule.MIMETypeNotSupportedException;
import HTTP.server.user.UserManager;
import HTTP.server.user.exception.PasswordException;
import HTTP.server.user.exception.PasswordFormatException;
import HTTP.server.user.exception.UserNotExistsException;
import HTTP.server.user.exception.UsernameFormatException;
import TCP.TCPServer;
import utils.EncodingUtil;
import utils.FileUtil;
import utils.JSON;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.function.Function;

public class HTTPServer extends TCPServer {
    private static final String ROOT_PATH = System.getProperty("user.dir") + File.separator + "root" + File.separator;
    private static final String DEFAULT_FILE_PATH = ROOT_PATH + "welcome.txt";
    private static final String MSG_BODY_PATH = ROOT_PATH + "msgbody" + File.separator;
    private static final String SERVER_NAME = "Simple HTTP Server";
    private final HashMap<String, Function<HTTPRequest, HTTPResponse>> routerMap = new HashMap<>();

    {
        routerMap.put("/", this::handleDefault);
        routerMap.put("/register", this::handleRegister);
        routerMap.put("/login", this::handleLogin);
        routerMap.put("/document", this::handleDocument);
        routerMap.put("/logout", this::handleLogout);
    }

    public HTTPServer(int port) {
        super(port);
    }

    public void run() {
        run(bytes -> {
            if (bytes == null || bytes.length == 0) return null;
            return handleRequest(bytes);
        });
    }

    private byte[] handleRequest(byte[] message) {
        String messageStr = EncodingUtil.decodeBinary(message);
        HTTPRequest request;
        try {
            request = new HTTPRequest(messageStr);
            new Thread(() -> showReceivedMessage.accept(request)).start();
        } catch (HTTPRequestFormatException e) {
            return handleBadRequest().getBytes();
        } catch (HTTPMethodNotAllowedException e) {
            return handleMethodNotAllowed(e.getMessage()).getBytes();
        }
        String path = getRouter(request.getRequestLine().getPath());
        if (!routerMap.containsKey(path)) {
            return handleNotFound().getBytes();
        }
        HTTPResponse response = routerMap.get(path).apply(request);
        new Thread(() -> showSentMessage.accept(response)).start();
        return response.getBytes();
    }

    private String getRouter(String path) {
        int index = path.indexOf('/', 1);
        return index == -1 ? path : path.substring(0, index);
    }

    private HTTPResponse handleDefault(HTTPRequest request) {
        if ((!request.getRequestLine().getMethod().equals("GET"))) {
            return handleMethodNotAllowed("Only GET method is allowed");
        }

        try {
            HTTPResponse response = new HTTPResponse();
            byte[] content = FileUtil.read(DEFAULT_FILE_PATH);

            response.getStatusLine().setVersion(request.getRequestLine().getVersion());
            response.getStatusLine().setStatusCode(200);
            response.getHeaders().add("Content-Type", MIME.getType(FileUtil.getExtension(DEFAULT_FILE_PATH)));
            response.getHeaders().add("Content-Length", String.valueOf(content.length));
            response.getHeaders().add("Server", SERVER_NAME);
            response.getBody().setBody(content);
            return response;
        } catch (FileNotFoundException e) {
            return handleNotFound();
        } catch (IOException | HTTPResponseFormatException | MIMETypeNotSupportedException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleRegister(HTTPRequest request) {
        if (!request.getRequestLine().getMethod().equals("POST")) {
            return handleMethodNotAllowed("Only POST method is allowed");
        }
        if (!request.getRequestLine().getPath().equals("/register")) {
            return handleNotFound();
        }

        try {
            if (!request.getHeaders().get("Content-Type").equals(MIME.getType("json"))) {
                return handleBadRequest("Only json file is accepted", "txt");
            }

            JSON userInfo = new JSON(EncodingUtil.decodeBinary(request.getBody().getBytes()));
            if (!userInfo.contains("username") || !userInfo.contains("password")) {
                return handleBadRequest("username and password are required", "txt");
            }

            String username = userInfo.get("username");
            String password = userInfo.get("password");
            if (!UserManager.register(username, password)) {
                JSON json = new JSON();
                json.add("error", "conflict");
                json.add("conflict", "user existed");
                return handleConflict(json.toString(), "json");
            }

            initUserSpace(username);
            JSON json = new JSON();
            json.add("success", "true");
            return userSuccess(json);
        } catch (HTTPResponseFormatException | MIMETypeNotSupportedException | UserNotExistsException e) {
            return handleInternalServerError();
        } catch (PasswordFormatException e) {
            JSON json = new JSON();
            json.add("error", "password");
            json.add("password", e.getMessage());
            return handleBadRequest(json.toString(), "json");
        } catch (UsernameFormatException e) {
            JSON json = new JSON();
            json.add("error", "username");
            json.add("username", e.getMessage());
            return handleBadRequest(json.toString(), "json");
        }
    }

    private HTTPResponse handleLogin(HTTPRequest request) {
        if (!request.getRequestLine().getPath().equals("/login")) {
            return handleNotFound();
        }

        try {
            if (request.getRequestLine().getMethod().equals("POST")) {
                return login(request);
            }

            if (!request.getRequestLine().getMethod().equals("GET")) {
                return handleMethodNotAllowed("Only GET and POST methods are allowed");
            }

            return handleGetLogin(request);
        } catch (HTTPResponseFormatException | MIMETypeNotSupportedException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleGetLogin(HTTPRequest request) throws HTTPResponseFormatException, MIMETypeNotSupportedException {
        HTTPResponse response = new HTTPResponse();
        response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
        response.getStatusLine().setStatusCode(200);

        byte[] content= EncodingUtil.encodeText("You need to login first");
        if (request.getHeaders().contains("Authorization")) {
            String username = UserManager.getUsername(request.getHeaders().get("Authorization"));
            if (username != null) {
                content = EncodingUtil.encodeText("Welcome, " + username + "!");
            }
        }

        response.getHeaders().add("Content-Type", MIME.getType("json"));
        response.getHeaders().add("Content-Length", String.valueOf(content.length));
        response.getHeaders().add("Server", SERVER_NAME);
        response.getBody().setBody(content);
        return response;
    }

    private HTTPResponse login(HTTPRequest request) {
        if (request.getHeaders().contains("Authorization") &&
            UserManager.isLoggedIn(request.getHeaders().get("Authorization"))) {
            return handleConflict("You are logged in now, please logout and then try again.", "txt");
        }

        try {
            if (!request.getHeaders().get("Content-Type").equals(MIME.getType("json"))) {
                return handleBadRequest("Only json file is accepted", "txt");
            }

            JSON userInfo = new JSON(EncodingUtil.decodeBinary(request.getBody().getBytes()));
            if (!userInfo.contains("username") || !userInfo.contains("password")) {
                return handleBadRequest("username and password are required", "txt");
            }

            String username = userInfo.get("username");
            String password = userInfo.get("password");
            String token = UserManager.login(username, password);
            if (token == null) {
                JSON json = new JSON();
                json.add("error", "username");
                json.add("username", "user logged in");
                return handleConflict(json.toString(), "json");
            }

            JSON json = new JSON();
            json.add("success", "true");
            json.add("token", token);
            return userSuccess(json);
        } catch (MIMETypeNotSupportedException | HTTPResponseFormatException e) {
            return handleInternalServerError();
        } catch (PasswordException e) {
            JSON json = new JSON();
            json.add("error", "password");
            json.add("password", "password error");
            return handleBadRequest(json.toString(), "json");
        } catch (UserNotExistsException e) {
            JSON json = new JSON();
            json.add("error", "username");
            json.add("username", "user not exists");
            return handleBadRequest(json.toString(), "json");
        }
    }

    private HTTPResponse handleLogout(HTTPRequest request) {
        if (!request.getRequestLine().getMethod().equals("POST")) {
            return handleMethodNotAllowed("Only POST method is allowed");
        }

        if (!request.getHeaders().contains("Authorization")) {
            return handleBadRequest("Not logged in", "txt");
        }

        String token = request.getHeaders().get("Authorization");
        if (!UserManager.logout(token)) {
            return handleConflict("user logged out", "txt");
        }

        JSON json = new JSON();
        json.add("success", "true");
        try {
            return userSuccess(json);
        } catch (HTTPResponseFormatException | MIMETypeNotSupportedException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse userSuccess(JSON json)
            throws HTTPResponseFormatException, MIMETypeNotSupportedException {
            HTTPResponse response = new HTTPResponse();
            byte[] content = json.getBytes();
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(200);
            response.getHeaders().add("Content-Type", MIME.getType("json"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length));
            response.getHeaders().add("Server", SERVER_NAME);
            response.getBody().setBody(content);
            return response;
    }

    private HTTPResponse handleDocument(HTTPRequest request) {
        try {
            String method = request.getRequestLine().getMethod();
            if (!method.equals("GET") && !method.equals("POST")) {
                return handleMethodNotAllowed("Only GET and POST methods are allowed");
            }

            if (!request.getHeaders().contains("Authorization")) {
                return handleFound("/login");
            }

            String token = request.getHeaders().get("Authorization");
            String path = request.getRequestLine().getPath();
            if (!checkUserSpace(token)) {
                return handleInternalServerError();
            }

            if (!token.equals(UserManager.getRootToken())) {
                String userDir = UserManager.getUserDirByToken(token);
                if (userDir == null) {
                    return handleFound("/login");
                }
                path = path.replaceAll("(document)", "$1/" + userDir);
            }

            if (method.equals("POST")) {
                return handlePostFile(path, request);
            }

            File file = new File(ROOT_PATH + path);

            if (!file.exists()) {
                return handleNotFound();
            }

            if (file.isDirectory()) {
                return handleGetDir(path, request);
            } else {
                return handleGetFile(path, request);
            }
        } catch (HTTPResponseFormatException | IOException | MIMETypeNotSupportedException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleGetFile(String path, HTTPRequest request) throws IOException, MIMETypeNotSupportedException, HTTPResponseFormatException {
        String clientPath = request.getRequestLine().getPath();
        if (clientPath.endsWith("/")) {
            return handleMovedPermanently(clientPath.substring(0, clientPath.length() - 1));
        }

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

        HTTPResponse response = new HTTPResponse();

        response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
        response.getStatusLine().setStatusCode(200);
        response.getHeaders().add("Content-Type", MIME.getType(extension));
        response.getHeaders().add("Content-Length", String.valueOf(content.length));
        response.getHeaders().add("Last-Modified", timestamp);
        response.getHeaders().add("Cache-Control", "no-cache");
        response.getHeaders().add("Server", SERVER_NAME);
        response.getBody().setBody(content);
        return response;
    }

    private HTTPResponse handlePostFile(String path, HTTPRequest request) throws IOException {
        String clientPath = request.getRequestLine().getPath();
        File file = new File(ROOT_PATH + path);

        if (file.isDirectory()) {
            return handleBadRequest();
        }

        byte[] content = request.getBody().getBytes();
        FileUtil.write(ROOT_PATH + path, content);
        return handleFound(clientPath.substring(0, clientPath.lastIndexOf("/") + 1));
    }

    private HTTPResponse handleGetDir(String path, HTTPRequest request) throws MIMETypeNotSupportedException, HTTPResponseFormatException {
        String clientPath = request.getRequestLine().getPath();
        if (!clientPath.endsWith("/")) {
            return handleMovedPermanently(clientPath + "/");
        }

        try {
            String content = FileUtil.listFiles(ROOT_PATH + path);
            HTTPResponse response = new HTTPResponse();
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(200);
            response.getHeaders().add("Content-Type", MIME.getType("txt"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length()));
            response.getHeaders().add("Server", SERVER_NAME);
            response.getBody().setBody(EncodingUtil.encodeText(content));
            return response;
        } catch (FileNotFoundException e) {
            return handleNotFound();
        }

    }

    private void initUserSpace(String username) throws UserNotExistsException {
        String userDir = UserManager.getUserDirByName(username);
        if (userDir == null) {
            throw new UserNotExistsException();
        }
        File userSpace = new File(ROOT_PATH + "document" + File.separator + userDir);
        userSpace.mkdirs();
    }

    private boolean checkUserSpace(String token) {
        String userDir = UserManager.getUserDirByToken(token);
        if (userDir == null) {
            return true;
        }

        File userSpace = new File(ROOT_PATH + "document" + File.separator + userDir);
        return userSpace.exists();
    }

    private HTTPResponse handleBadRequest(String message, String type) {
        try {
            HTTPResponse response = new HTTPResponse();
            byte[] content = EncodingUtil.encodeText(message);
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(400);
            response.getHeaders().add("Content-Type", MIME.getType(type));
            response.getHeaders().add("Content-Length", String.valueOf(content.length));
            response.getHeaders().add("Server", SERVER_NAME);
            response.getBody().setBody(content);
            return response;
        } catch (HTTPResponseFormatException | MIMETypeNotSupportedException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleBadRequest() {
        try {
            byte[] content = FileUtil.read(MSG_BODY_PATH + "400.txt");
            return handleBadRequest(EncodingUtil.decodeText(content), "txt");
        } catch (IOException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleNotFound() {
        try {
            HTTPResponse response = new HTTPResponse();
            byte[] content = FileUtil.read(MSG_BODY_PATH + "404.txt");

            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(404);
            response.getHeaders().add("Content-Type", MIME.getType("txt"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length));
            response.getHeaders().add("Server", SERVER_NAME);
            response.getBody().setBody(content);
            return response;
        } catch (HTTPResponseFormatException | IOException | MIMETypeNotSupportedException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleInternalServerError() {
        try {
            HTTPResponse response = new HTTPResponse();
            byte[] content = FileUtil.read(MSG_BODY_PATH + "500.txt");

            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(500);
            response.getHeaders().add("Content-Type", MIME.getType("txt"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length));
            response.getHeaders().add("Server", SERVER_NAME);
            response.getBody().setBody(content);
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HTTPResponse handleMethodNotAllowed(String detail) {
        HTTPResponse response = new HTTPResponse();

        try {
            String content = EncodingUtil.decodeText(FileUtil.read(MSG_BODY_PATH + "405.txt"));
            content = String.format(content, detail);

            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(405);
            response.getHeaders().add("Content-Type", MIME.getType("txt"));
            response.getHeaders().add("Content-Length", String.valueOf(content.length()));
            response.getHeaders().add("Server", SERVER_NAME);
            response.getBody().setBody(EncodingUtil.encodeText(content));
            return response;
        } catch (IOException | IllegalFormatException | HTTPResponseFormatException | MIMETypeNotSupportedException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleMovedPermanently(String path) {
        try {
            HTTPResponse response = new HTTPResponse();
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(301);
            response.getHeaders().add("Location", path);
            response.getHeaders().add("Content-Length", "0");
            response.getHeaders().add("Server", SERVER_NAME);
            return response;
        } catch (HTTPResponseFormatException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleFound(String location) {
        try {
            HTTPResponse response = new HTTPResponse();
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(302);
            response.getHeaders().add("Location", location);
            response.getHeaders().add("Content-Length", "0");
            response.getHeaders().add("Server", SERVER_NAME);
            return response;
        } catch (HTTPResponseFormatException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleNotModified(HTTPRequest request) {
        try {
            HTTPResponse response = new HTTPResponse();
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(304);
            response.getHeaders().add("Content-Length", "0");
            response.getHeaders().add("Last-Modified", request.getHeaders().get("If-Modified-Since"));
            response.getHeaders().add("Cache-Control", "no-cache");
            response.getHeaders().add("Server", SERVER_NAME);
            return response;
        } catch (HTTPResponseFormatException e) {
            return handleInternalServerError();
        }
    }

    private HTTPResponse handleConflict(String message, String type) {
        try {
            HTTPResponse response = new HTTPResponse();
            byte[] content = EncodingUtil.encodeText(message);
            response.getStatusLine().setVersion(HTTPVersion.getDefaultVersion());
            response.getStatusLine().setStatusCode(409);
            response.getHeaders().add("Content-Type", MIME.getType(type));
            response.getHeaders().add("Content-Length", String.valueOf(content.length));
            response.getHeaders().add("Server", SERVER_NAME);
            response.getBody().setBody(content);
            return response;
        } catch (HTTPResponseFormatException | MIMETypeNotSupportedException e) {
            return handleInternalServerError();
        }
    }
}