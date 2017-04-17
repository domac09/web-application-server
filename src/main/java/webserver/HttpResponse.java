package webserver;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpStatusCode;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by iseongho on 2017. 4. 13..
 */
public class HttpResponse {
    private static final Logger log = LoggerFactory.getLogger(HttpResponse.class);
    private final Map<String, String> header;
    Map<String, String> params = new HashMap<>();
    String path;
    HttpRequest request;

    public HttpResponse(HttpRequest request, OutputStream out) throws IOException {
        this.request = request;
        this.params = request.params;
        this.path = request.path;
        this.header = request.header;

        DataOutputStream dos = new DataOutputStream(out);
        HttpStatusCode response = controller(this.params, this.path);

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

            log.info("cookie => {}", this.header.get("Cookie"));
            if (this.header.get("Cookie") == null) {
                return new HttpStatusCode(401, "/user/login_failed.html", "login=false");
            }else{
                if ("login=false".startsWith(this.header.get("Cookie"))) {
                    return new HttpStatusCode(401, "/user/login_failed.html", "login=false");
                }
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
