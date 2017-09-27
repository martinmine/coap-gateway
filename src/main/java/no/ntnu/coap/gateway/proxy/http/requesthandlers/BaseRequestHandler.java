package no.ntnu.coap.gateway.proxy.http.requesthandlers;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The Class BaseRequestHandler handles simples requests that do not
 * need the proxying.
 */
public class BaseRequestHandler implements HttpRequestHandler {

    private static final Logger LOGGER = Logger.getLogger(BaseRequestHandler.class.getName());

    /*
     * (non-Javadoc)
     * @see
     * org.apache.http.protocol.HttpRequestHandler#handle(org.apache
     * .http .HttpRequest, org.apache.http.HttpResponse,
     * org.apache.http.protocol.HttpContext)
     */
    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
        httpResponse.setStatusCode(HttpStatus.SC_OK);
        httpResponse.setEntity(new StringEntity("Californium Proxy server"));

        LOGGER.finer("Root request handled");
    }
}