package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static util.HttpRequestUtils.parseQueryString;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        // stream의 경우 try 구문에 선언을 하면 Closeable interface의 close구문이 자동으로 실행된다. jdk 1.7 문법
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            String line = br.readLine();
            log.debug("request line = {}", line);

            if(line == null){
                return;
            }

            String[] tokens = line.split(" ");

            while(!"".equals(line)){
                line = br.readLine();
                log.debug("header : {}", line);
            }

            String url = tokens[1];
            int index = url.indexOf("?");
            String requestPath = url.substring(0, index);
            String params = url.substring(index+1);

            Map<String, String> parseQueryString = parseQueryString(params);

            User user = createUser(parseQueryString);

            log.debug("User is = {}", user);

            byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());

            DataOutputStream dos = new DataOutputStream(out);
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private User createUser(Map<String, String> parseQueryString) {
        return new User(parseQueryString.get("userId"), parseQueryString.get("password"), parseQueryString.get("name"), parseQueryString.get("email"));
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
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
