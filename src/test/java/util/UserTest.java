package util;

import model.User;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static util.HttpRequestUtils.parseQueryString;

/**
 * Created by leeseongho on 2017. 3. 24..
 */
public class UserTest {
    private static final Logger logger = LoggerFactory.getLogger(UserTest.class);


    String url = "/user/create?userId=1123&password=11&name=11&email=11%40gmail.com";

    @Test
    public void readData() throws Exception {
        String url = this.url;
        int index = url.indexOf("?");
        String requestPath = url.substring(0, index);
        String params = url.substring(index + 1);

        Map<String, String> parseQueryString = parseQueryString(params);

        logger.info("{}", parseQueryString);

        assertThat(parseQueryString.get("userId"), is("1123"));
    }


    @Test
    public void CanNotGetUserByGet() throws Exception {
        String url = "/user/create";
        String[] params = url.split("\\?");

        assertThat(params.length, is(1));
    }
}
