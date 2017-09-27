package no.ntnu.coap.gateway.proxy.http.requesthandlers;

import no.ntnu.coap.gateway.proxy.HttpTranslator;
import no.ntnu.coap.gateway.proxy.InvalidFieldException;
import no.ntnu.coap.gateway.proxy.InvalidMethodException;
import no.ntnu.coap.gateway.proxy.TranslationException;
import no.ntnu.coap.gateway.proxy.http.RequestContext;
import no.ntnu.coap.gateway.proxy.http.RequestHandler;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.eclipse.californium.core.coap.Request;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Class associated with the http service to translate the http requests
 * in coap requests and to produce the http responses. Even if the class
 * accepts a string indicating the name of the proxy resource, it is
 * still thread-safe because the local resource is set in the
 * constructor and then only read by the methods.
 */
public class ProxyAsyncRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {

    private final String localResource;
    private final boolean proxyingEnabled;
    private final RequestHandler requestHandler;

    private static final Logger LOGGER = Logger.getLogger(ProxyAsyncRequestHandler.class.getName());

    /**
     * Instantiates a new proxy request handler.
     *
     * @param localResource   the local resource
     * @param proxyingEnabled
     */
    public ProxyAsyncRequestHandler(String localResource, boolean proxyingEnabled, RequestHandler requestHandler) {
        super();

        this.localResource = localResource;
        this.proxyingEnabled = proxyingEnabled;
        this.requestHandler = requestHandler;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.http.nio.protocol.HttpAsyncRequestHandler#handle(java.
     * lang.Object, org.apache.http.nio.protocol.HttpAsyncExchange,
     * org.apache.http.protocol.HttpContext)
     */
    @Override
    public void handle(HttpRequest httpRequest, HttpAsyncExchange httpExchange, HttpContext httpContext) throws HttpException, IOException {
        LOGGER.finer("Incoming http request: " + httpRequest.getRequestLine());
        final RequestContext context = new RequestContext(httpExchange, httpRequest);

        try {
            // translate the request in a valid coap request
            Request coapRequest = HttpTranslator.getCoapRequest(httpRequest, localResource, proxyingEnabled);
            LOGGER.info("Received HTTP request and translate to " + coapRequest);
            LOGGER.finer("Fill exchange with: " + coapRequest + " with hash=" + coapRequest.hashCode());

            requestHandler.handleRequest(coapRequest, context);
        } catch (InvalidMethodException e) {
            LOGGER.warning("Method not implemented" + e.getMessage());
            context.sendSimpleHttpResponse(HttpTranslator.STATUS_WRONG_METHOD);
        } catch (InvalidFieldException e) {
            LOGGER.warning("Request malformed" + e.getMessage());
            context.sendSimpleHttpResponse(HttpTranslator.STATUS_URI_MALFORMED);
        } catch (TranslationException e) {
            LOGGER.warning("Failed to translate the http request in a valid coap request: " + e.getMessage());
            context.sendSimpleHttpResponse(HttpTranslator.STATUS_TRANSLATION_ERROR);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.http.nio.protocol.HttpAsyncRequestHandler#processRequest
     * (org.apache.http.HttpRequest,
     * org.apache.http.protocol.HttpContext)
     */
    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        // Buffer request content in memory for simplicity
        return new BasicAsyncRequestConsumer();
    }
}
