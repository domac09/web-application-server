package webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.IOUtils;

import java.io.*;
import java.net.Socket;
import java.util.*;

import static util.HttpRequestUtils.parseQueryString;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        // stream의 경우 try 구문에 선언을 하면 Closeable interface의 close 구문이 자동으로 실행된다. jdk 1.7 문법
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            HttpRequest request = new HttpRequest(in);

            new HttpResponse(request, out);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
