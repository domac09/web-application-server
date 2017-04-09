package webserver;

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
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        // stream의 경우 try 구문에 선언을 하면 Closeable interface의 close 구문이 자동으로 실행된다. jdk 1.7 문법
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            String line = br.readLine();

            if (line == null) {
                return;
            }

            log.debug("line => {}", line);

            String path = getPath(line);

            while (!"".equals(line)) {
                line = br.readLine();
                log.debug("header => {}", line);
            }

            // user cleare logic
            String[] params = path.split("\\?");


            log.debug("params => {}", params[1]);
            String[] userValue = params[1].split("&");
            Map<String, String> map = new HashMap<>();

            for (String args : userValue) {
                String[] splitParams = args.split("=");
                map.put(splitParams[0], splitParams[1]);
            }

            log.debug("converted map of params => {}", map);


            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());

            log.debug("path => {}", path);
            int index = path.lastIndexOf(".");
            String extension = path.substring(index + 1);

            response200Header(dos, body.length, extension);

            responseBody(dos, body);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String getPath(String line) {
        String[] split = line.split(" ");
        return split[1];
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
