package webserver;

import webserver.Controller.*;

import static webserver.enums.HttpUrl.*;

public class RequestMapper {

    static Controller getController(HttpRequest request) {
        String path = request.getPath();

        if (path.equals("/")) {
            return new HomeController();
        }
        if (path.endsWith(".html")) {
            return new ForwardController();
        }
        if (path.startsWith(HTTP_USER_SIGNUP.getValue())) {
            return new SignUpController();
        }
        if (path.equals("/user/login")) {
            return new LoginController();
        }
        if (path.equals(HTTP_USER_LIST.getValue())) {
            return new ListController();
        }
        if (path.endsWith(".css")) {
            return new CssController();
        }

        return new ForwardController();
    }
}
