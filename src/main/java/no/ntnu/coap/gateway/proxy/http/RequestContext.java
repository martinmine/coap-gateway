package no.ntnu.coap.gateway.proxy.http;


import no.ntnu.coap.gateway.proxy.HttpTranslator;
import no.ntnu.coap.gateway.proxy.TranslationException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * The Class RequestContext. This thread waits a response from the lower
 * layers. It is the consumer of the producer/consumer pattern.
 */
public final class RequestContext {
    private final HttpAsyncExchange httpExchange;
    private final HttpRequest httpRequest;

    private static final Logger LOGGER = Logger.getLogger(RequestContext.class.getName());

    /**
     * Instantiates a new coap response worker.
     *
     * @param httpExchange the http exchange
     * @param httpRequest  the http request
     */
    public RequestContext(HttpAsyncExchange httpExchange, HttpRequest httpRequest) {
        // super(name);
        this.httpExchange = httpExchange;
        this.httpRequest = httpRequest;
    }

    public void handleRequestForwarding(final Response coapResponse) {
        if (coapResponse == null) {
            LOGGER.warning("No coap response");
            sendSimpleHttpResponse(HttpTranslator.STATUS_NOT_FOUND);
            return;
        }

        // get the sample http response
        HttpResponse httpResponse = httpExchange.getResponse();

        try {
            // translate the coap response in an http response
            HttpTranslator.getHttpResponse(httpRequest, coapResponse, httpResponse);

            LOGGER.info("<-- " + httpRequest.getRequestLine().getUri() + " HTTP " + httpResponse.getStatusLine().getStatusCode());
        } catch (TranslationException e) {
            LOGGER.warning("Failed to translate coap response to http response: " + e.getMessage());
            sendSimpleHttpResponse(HttpTranslator.STATUS_TRANSLATION_ERROR);
            return;
        }

        // send the response
        httpExchange.submitResponse();
    }

    /**
     * Send simple http response.
     *
     * @param httpCode the http code
     */
    public void sendSimpleHttpResponse(int httpCode) {
        // get the empty response from the exchange
        HttpResponse httpResponse = httpExchange.getResponse();

        // create and set the status line
        StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, httpCode, EnglishReasonPhraseCatalog.INSTANCE.getReason(httpCode, Locale.ENGLISH));
        httpResponse.setStatusLine(statusLine);

        LOGGER.info("<-- HTTP " + httpCode + " for URL [" + httpRequest.getRequestLine().getUri() + "]");
        // send the error response
        httpExchange.submitResponse();
    }
}