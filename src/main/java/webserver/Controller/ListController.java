package webserver.Controller;

import webserver.*;
import java.io.IOException;
import static webserver.enums.HttpHeader.*;
import static webserver.enums.HttpUrl.*;

public class ListController implements Controller {
    @Override
    public void execute(HttpRequest request, HttpResponse response) throws IOException {
        //로그인 되어있으면 리스트 HTML 전송
        if (isLoggedIn(request)) {
            response.forward(HTTP_LIST_HTML.getValue());
        } else {
            response.forward(HTTP_LOGIN_HTML.getValue());
        }
    }

    private static boolean isLoggedIn(HttpRequest request) {
        boolean isLoggedIn = request.getHeader(COOKIE.getValue()) != null &&
                request.getHeader(COOKIE.getValue()).contains("logined=true");
        return isLoggedIn;
    }
}
