package util;

/**
 * Created by iseongho on 2017. 4. 10..
 */
public class HttpStatusCode {
    private int statusCode;
    private String message;
    private String cookies;

    public HttpStatusCode(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public HttpStatusCode(int statusCode, String message, String cookies) {
        this.statusCode = statusCode;
        this.message = message;
        this.cookies = cookies;
    }

    public HttpStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public String getCookies() {
        return cookies;
    }
}
