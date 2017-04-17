package webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by iseongho on 2017. 4. 13..
 */
public class HttpRequest {
    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);

    String method;
    String path;
    Map<String, String> params = new HashMap<>();
    Map<String, String> header = new HashMap<>();


    public HttpRequest(InputStream in) {
        BufferedReader br;

        try {
            br = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            processRequestLine(br);
            requestHeader(br);
            requestBody(br);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processRequestLine(BufferedReader br) throws IOException {
        String line = br.readLine();

        if (line == null) {
            return;
        }

        requestLine(line);
    }

    private void requestLine(String line) {
        String[] split = line.split(" ");
        this.method = split[0];

        String[] urls = split[1].split("\\?");
        this.path = urls[0];

        if (urls.length == 2) {
            this.params = HttpRequestUtils.parseQueryString(urls[1]);
        }
    }

    private void requestHeader(BufferedReader br) throws IOException {
        String line;

        while (!(line = br.readLine()).equals("")) {
            log.debug("header => {}", line);
            String[] splitContentLength = line.split(": ");
            header.put(splitContentLength[0], splitContentLength[1]);
        }
    }

    private void requestBody(BufferedReader br) throws IOException {
        String contentLength = header.get("Content-Length");
        if (contentLength != null) {
            String body = IOUtils.readData(br, Integer.parseInt(contentLength));
            this.params = HttpRequestUtils.parseQueryString(body);
        }
    }

}
