package webserver;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpStatusCode;
import util.IOUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
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
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            String line = br.readLine();

            if (line == null) {
                return;
            }

            Map<String, String> pathMap = getPath(line);
            int contentLength = 0;
            String cookie = null;

            while (!"".equals(line)) {
                line = br.readLine();
                log.debug("header => {}", line);
                if (line.startsWith("Content-Length")) {
                    String[] splitContentLength = line.split(":");
                    contentLength = Integer.parseInt(splitContentLength[1].trim());

                }

                if (line.startsWith("Cookie")) {
                    String[] splitCookiValue = line.split(":");
                    cookie = splitCookiValue[1].trim();
                }
            }

            String method = pathMap.get("method");
            String path = pathMap.get("path");
            Map<String, String> parameter = getParameter(br, contentLength, path, method);
            parameter.put("cookie", cookie);

            DataOutputStream dos = new DataOutputStream(out);

            HttpStatusCode response = controller(parameter, path);

            if (response.getStatusCode() == 200) {
                byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());

                int index = path.lastIndexOf(".");
                String extension = path.substring(index + 1);

                response200Header(dos, body.length, extension);

                responseBody(dos, body);
            }

            if (response.getStatusCode() == 303) {
                response303HeaderWithCookie(dos, response.getMessage(), response.getCookies());
            }

            if (response.getStatusCode() == 401) {
                response303HeaderWithCookie(dos, response.getMessage(), response.getCookies());
            }

            if (response.getStatusCode() == 201) {
                response302Header(dos, response.getMessage());
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private Map<String, String> getParameter(BufferedReader br, int contentLength, String path, String method) throws IOException {
        if (method.equals("GET")) {
            String[] getParams = path.split("\\?");
            if (getParams.length > 1) {
                return parseQueryString(getParams[1]);
            }
        }

        if (method.equals("POST")) {
            return parseQueryString(IOUtils.readData(br, contentLength));
        }

        return new HashMap<>();
    }

    private HttpStatusCode controller(Map<String, String> parameter, String path) {

        if (path.equals("/user/create")) {
            DataBase.addUser(
                    userCreate(
                            parameter.get("userId"),
                            parameter.get("password"),
                            parameter.get("name"),
                            parameter.get("email")
                    )
            );

            return new HttpStatusCode(201, "/index.html");
        }

        if (path.equals("/user/login")) {
            User user = DataBase.findUserById(parameter.get("userId"));

            if (user == null || !user.getPassword().equals(parameter.get("password"))) {
                return new HttpStatusCode(401, "/user/login_failed.html", "login=false");
            }

            return new HttpStatusCode(303, "/index.html", "login=true");
        }

        if (path.equals("/user/list")) {

            if (parameter.get("cookie").contains("login=false")) {
                return new HttpStatusCode(401, "/user/login_failed.html", "login=false");
            }

            Collection<User> all = DataBase.findAll();

            for (User user : all) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(user.getUserId() + "||");
                stringBuilder.append(user.getPassword() + "||");
                stringBuilder.append(user.getName() + "||");
                stringBuilder.append(user.getEmail() + ";");

                log.debug("user list => {}", stringBuilder);
            }

        }

        return new HttpStatusCode(200);
    }

    private User userCreate(String userId, String password, String name, String email) {
        return new User(userId, password, name, email);
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
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: " + path + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response303HeaderWithCookie(DataOutputStream dos, String path, String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 303 See Other \r\n");
            dos.writeBytes("Location: " + path + "\r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
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
