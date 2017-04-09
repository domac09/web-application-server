package webserver;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.IOUtils;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static util.HttpRequestUtils.parseQueryString;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
//        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        // stream의 경우 try 구문에 선언을 하면 Closeable interface의 close 구문이 자동으로 실행된다. jdk 1.7 문법
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            String line = br.readLine();

            if (line == null) {
                return;
            }

            log.debug("line => {}", line);

            Map<String, String> path = getPath(line);

            int contentLength = 0;

            while (!"".equals(line)) {
                line = br.readLine();
                log.debug("header => {}", line);

                if (line.startsWith("Content-Length")) {
                    String[] splitContentLength = line.split(":");
                    contentLength = Integer.parseInt(splitContentLength[1].trim());
                }
            }

            String url = path.get("url");
            if (url.startsWith("/user/create")) {

                User user = null;
                String method = path.get("method");
                if (method.equals("GET")) {
                    String[] userParams = url.split("\\?");
                    if (userParams.length > 1) {
                        user = userCreate(userParams[1]);
                    }
                }

                if (method.equals("POST")) {
                    String userParams = IOUtils.readData(br, contentLength);
                    user = userCreate(userParams);
                }
                log.debug("User created => {}", user);
            }

            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());

            int index = url.lastIndexOf(".");
            String extension = url.substring(index + 1);

            response200Header(dos, body.length, extension);

            responseBody(dos, body);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private User userCreate(String data) throws UnsupportedEncodingException {
        Map<String, String> parseQueryString = parseQueryString(URLDecoder.decode(data, "UTF-8"));
        return new User(parseQueryString.get("userId"), parseQueryString.get("password"), parseQueryString.get("name"), parseQueryString.get("email"));
    }

    private Map<String, String> getPath(String line) {
        String[] split = line.split(" ");

        Map<String, String> map = new HashMap<>();
        map.put("method", split[0]);
        map.put("url", split[1]);

        return map;
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String extension) {

        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: " + getContentType(extension) + "\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String getContentType(String extension) {
        switch (extension) {
            case "css":
                return "text/css;charset=utf-8";
            case "js":
                return "text/javascript;charset=utf-8";
            case "woff":
                return "font/woff";
            case "woff2":
                return "font/woffw";
            case "png":
                return "image/png";
        }
        return "text/html;charset=utf-8";
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
