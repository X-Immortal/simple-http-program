package HTTP.client;

import HTTP.message.HTTPRequest;
import HTTP.message.HTTPResponse;
import HTTP.message.exception.HTTPMethodNotAllowedException;
import HTTP.message.exception.HTTPRequestFormatException;
import HTTP.message.exception.HTTPResponseFormatException;
import HTTP.rule.HTTPVersion;
import HTTP.rule.MIME;
import HTTP.rule.MIMETypeNotSupportedException;
import HTTP.server.user.UserManager;
import utils.EncodingUtil;
import utils.FileUtil;
import TCP.TCPClient;

import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.util.HashMap;
import java.util.function.BiConsumer;

public final class HTTPClient extends TCPClient {
    private static final String HOST_NAME = "Simple HTTP Client";
    private final HashMap<String, String> redirectionMap = new HashMap<>();
    private final HashMap<String, File> cache = new HashMap<>();
    private String path;
    private String token = "";

    public HTTPClient(String host, int port, String path) {
        super(host, port);
        this.path = path;
    }

    public HTTPClient(URL url) {
        super(url);
        path = url.getPath();
        start();
    }

    private HTTPResponse getResponse(HTTPRequest request)
            throws IOException, HTTPResponseFormatException,
                HTTPRequestFormatException, HTTPMethodNotAllowedException {
        path = request.getRequestLine().getPath();
        path = redirectionMap.getOrDefault(path, path);
        request.getRequestLine().setPath(path);
        sendMessage(request.getBytes());

        HTTPResponse response = new HTTPResponse(EncodingUtil.decodeBinary(receiveMessage()));

        switch (response.getStatusLine().getStatusCode()) {
            case 301:
                redirectionMap.put(request.getRequestLine().getPath(), response.getHeaders().get("Location"));
            case 302:
                request.getRequestLine().setMethod("GET");
                request.getHeaders().add("Content-Length", "0");
                path = response.getHeaders().get("Location");
                request.getRequestLine().setPath(path);
                request.getBody().setBody(new byte[0]);
                return getResponse(request);
            case 304:
                return cache.get(request.getRequestLine().getPath()).getResponse();
            default:
                if (response.getHeaders().contains("Last-Modified")) {
                    cache.put(request.getRequestLine().getPath(), new File(response));
                }
                return response;
        }
    }

    public void enter(String path, BiConsumer<String, HTTPResponse> handler, boolean isRoot)
            throws HTTPMethodNotAllowedException, HTTPRequestFormatException,
            HTTPResponseFormatException, IOException {
        if (!isReady()) throw new SocketException("Not connected");

        HTTPRequest request = new HTTPRequest();
        request.getRequestLine().setMethod("GET");
        request.getRequestLine().setPath(path);
        request.getRequestLine().setVersion(HTTPVersion.getDefaultVersion());
        request.getHeaders().add("Host", HOST_NAME);
        request.getHeaders().add("Content-Length", "0");
        if (cache.containsKey(path)) {
            request.getHeaders().add("If-Modified-Since", cache.get(path).getTimestamp());
        }
        if (isRoot) {
            request.getHeaders().add("Authorization", UserManager.getRootToken());
        } else if (!token.isEmpty()) {
            request.getHeaders().add("Authorization", token);
        }
        HTTPResponse response = getResponse(request);
        handler.accept(this.path, response);
    }

    public void push(String path, byte[] data, BiConsumer<String, HTTPResponse> handler, boolean isRoot)
            throws HTTPMethodNotAllowedException, HTTPRequestFormatException,
            HTTPResponseFormatException, IOException, MIMETypeNotSupportedException,
            IllegalArgumentException {
        if (!isReady()) throw new SocketException("Not connected");

        if (path.endsWith("/")) {
            throw new IllegalArgumentException("Not a file");
        }

        HTTPRequest request = new HTTPRequest();
        request.getRequestLine().setMethod("POST");
        request.getRequestLine().setPath(path);
        request.getRequestLine().setVersion(HTTPVersion.getDefaultVersion());
        request.getHeaders().add("Host", HOST_NAME);
        request.getHeaders().add("Content-Type", MIME.getType(FileUtil.getExtension(path)));
        request.getHeaders().add("Content-Length", String.valueOf(data.length));
        if (isRoot) {
            request.getHeaders().add("Authorization", UserManager.getRootToken());
        } else if (!token.isEmpty()) {
            request.getHeaders().add("Authorization", token);
        }
        request.getBody().setBody(data);
        HTTPResponse response = getResponse(request);
        handler.accept(this.path, response);
    }

    public void login(String username, String password, BiConsumer<String, HTTPResponse> handler)
            throws HTTPMethodNotAllowedException, HTTPRequestFormatException,
            HTTPResponseFormatException, IOException, MIMETypeNotSupportedException,
            IllegalArgumentException {
        // TODO
        System.out.println("Not Implemented");
    }

    public void logout(BiConsumer<String, HTTPResponse> handler)
            throws HTTPMethodNotAllowedException, HTTPRequestFormatException,
                HTTPResponseFormatException, IOException, MIMETypeNotSupportedException,
                IllegalArgumentException {
        if (!isReady()) throw new SocketException("Not connected");

        HTTPRequest request = new HTTPRequest();
        request.getRequestLine().setMethod("POST");
        request.getRequestLine().setPath("/logout");
        request.getRequestLine().setVersion(HTTPVersion.getDefaultVersion());
        request.getHeaders().add("Host", HOST_NAME);
        request.getHeaders().add("Content-Length", "0");
        request.getHeaders().add("Authorization", token);
        HTTPResponse response = getResponse(request);
        if (response.getStatusLine().getStatusCode() == 200) {
            token = "";
        }
        handler.accept(this.path, response);
    }

    public void register(String username, String password, BiConsumer<String, HTTPResponse> handler)
            throws HTTPMethodNotAllowedException, HTTPRequestFormatException,
                HTTPResponseFormatException, IOException, MIMETypeNotSupportedException,
                IllegalArgumentException {
        // TODO
        System.out.println("Not Implemented");
    }
}