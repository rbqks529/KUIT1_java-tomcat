package webserver.Controller;

import webserver.HttpRequest;
import webserver.HttpResponse;
import java.io.IOException;

public interface Controller {
    void execute(HttpRequest request, HttpResponse response) throws IOException;
}
