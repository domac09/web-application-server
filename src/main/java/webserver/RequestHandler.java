package webserver;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.IOUtils;

import java.io.*;
import java.net.Socket;
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

            Map<String, String> pathMap = getPath(line);
            int contentLength = 0;

            while (!"".equals(line)) {
                line = br.readLine();
                log.debug("header => {}", line);

                if (line.startsWith("Content-Length")) {
                    String[] splitContentLength = line.split(":");
                    contentLength = Integer.parseInt(splitContentLength[1].trim());
                }
            }

            String method = pathMap.get("method");
            String path = pathMap.get("path");
            String parameter = getParameter(br, contentLength, path, method);

            int statusCode = controller(parameter, path);

            DataOutputStream dos = new DataOutputStream(out);

            if (statusCode == 200) {
                byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());

                int index = path.lastIndexOf(".");
                String extension = path.substring(index + 1);
                response200Header(dos, body.length, extension);

                responseBody(dos, body);
            }

            if (statusCode == 302) {
                response302Header(dos, "/index.html");
            }


        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String getParameter(BufferedReader br, int contentLength, String path, String method) throws IOException {
        if (method.equals("GET")) {
            String[] getParams = path.split("\\?");
            if (getParams.length > 1) {
                return getParams[1];
            }
        }

        if (method.equals("POST")) {
            return IOUtils.readData(br, contentLength);
        }

        return "";
    }

    private int controller(String parameter, String path) {
        if (path.startsWith("/user/create")) {
            DataBase.addUser(userCreate(parameter));

            return 302;
        }

        return 200;
    }

    private User userCreate(String data) {
        Map<String, String> parseQueryString = parseQueryString(data);
        return new User(parseQueryString.get("userId"), parseQueryString.get("password"), parseQueryString.get("name"), parseQueryString.get("email"));
    }

    private Map<String, String> getPath(String line) {
        String[] split = line.split(" ");

        Map<String, String> map = new HashMap<>();
        map.put("method", split[0]);
        map.put("path", split[1]);

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

    private void response302Header(DataOutputStream dos, String path) {

        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + path + "\r\n");
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
