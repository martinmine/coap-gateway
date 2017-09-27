/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 *    Martin Lanter - architect and re-implementation
 *    Francesco Corazza - HTTP cross-proxy
 ******************************************************************************/
package no.ntnu.coap.gateway.proxy.resources;

import no.ntnu.coap.gateway.proxy.*;
import no.ntnu.coap.gateway.proxy.http.HttpClientPool;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;


public class ProxyHttpClientResource extends ForwardingResource {
    private static final Logger LOGGER = Logger.getLogger(ProxyHttpClientResource.class.getName());
    public ProxyHttpClientResource() {
        // set the resource hidden
//		this("proxy/httpClient");
        this("httpClient");
    }

    public ProxyHttpClientResource(String name) {
        // set the resource hidden
        super(name, true);
        getAttributes().setTitle("Forward the requests to a HTTP client.");
    }

    @Override
    public CompletableFuture<Response> forwardRequest(Request request) {
        final CompletableFuture<Response> future = new CompletableFuture<>();
        final Request incomingCoapRequest = request;

        // check the invariant: the request must have the proxy-uri set
        if (!incomingCoapRequest.getOptions().hasProxyUri()) {
            LOGGER.warning("Proxy-uri option not set.");
            future.complete(new Response(ResponseCode.BAD_OPTION));
            return future;
        }

        // remove the fake uri-path // TODO: why? still necessary in new Cf?
        incomingCoapRequest.getOptions().clearUriPath(); // HACK

        // get the proxy-uri set in the incoming coap request
        URI proxyUri;
        try {
            String proxyUriString = URLDecoder.decode(
                    incomingCoapRequest.getOptions().getProxyUri(), "UTF-8");
            proxyUri = new URI(proxyUriString);
        } catch (UnsupportedEncodingException e) {
            LOGGER.warning("Proxy-uri option malformed: " + e.getMessage());
            future.complete(new Response(CoapTranslator.STATUS_FIELD_MALFORMED));
            return future;
        } catch (URISyntaxException e) {
            LOGGER.warning("Proxy-uri option malformed: " + e.getMessage());
            future.complete(new Response(CoapTranslator.STATUS_FIELD_MALFORMED));
            return future;
        }

        // get the requested host, if the port is not specified, the constructor
        // sets it to -1
        HttpHost httpHost = new HttpHost(proxyUri.getHost(), proxyUri.getPort(), proxyUri.getScheme());

        HttpRequest httpRequest;
        try {
            // get the mapping to http for the incoming coap request
            httpRequest = HttpTranslator.getHttpRequest(incomingCoapRequest);
            LOGGER.finer("--> --> " + httpRequest.getRequestLine());
        } catch (InvalidFieldException e) {
            LOGGER.warning("Problems during the http/coap translation: " + e.getMessage());
            future.complete(new Response(CoapTranslator.STATUS_FIELD_MALFORMED));
            return future;
        } catch (TranslationException e) {
            LOGGER.warning("Problems during the http/coap translation: " + e.getMessage());
            future.complete(new Response(CoapTranslator.STATUS_TRANSLATION_ERROR));
            return future;
        }


        // accept the request sending a separate response to avoid the timeout
        // in the requesting client
        LOGGER.finer("Acknowledge message sent");

        final CloseableHttpAsyncClient asyncClient = HttpClientPool.getClient();
        asyncClient.start();
        // execute the request

        asyncClient.execute(httpHost, httpRequest, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                HttpClientPool.putClient(asyncClient);
                long timestamp = System.nanoTime();
                //LOGGER.info("Incoming http response: " + result.getStatusLine());
                LOGGER.info("--> <-- " + httpRequest.getRequestLine().getUri() + " HTTP " + result.getStatusLine().getStatusCode());
                // the entity of the response, if non repeatable, could be
                // consumed only one time, so do not debug it!
                // System.out.println(EntityUtils.toString(httpResponse.getEntity()));

                // translate the received http response in a coap response
                try {
                    Response coapResponse = HttpTranslator.getCoapResponse(result, incomingCoapRequest);
                    coapResponse.setTimestamp(timestamp);

                    future.complete(coapResponse);
                } catch (InvalidFieldException e) {
                    LOGGER.warning("Problems during the http/coap translation: " + e.getMessage());
                    future.complete(new Response(CoapTranslator.STATUS_FIELD_MALFORMED));
                } catch (TranslationException e) {
                    LOGGER.warning("Problems during the http/coap translation: " + e.getMessage());
                    future.complete(new Response(CoapTranslator.STATUS_TRANSLATION_ERROR));
                }
            }

            @Override
            public void failed(Exception ex) {
                LOGGER.warning("Failed to get the http response: " + ex.getMessage());
                future.complete(new Response(ResponseCode.INTERNAL_SERVER_ERROR));
            }

            @Override
            public void cancelled() {
                LOGGER.warning("Request canceled");
                future.complete(new Response(ResponseCode.SERVICE_UNAVAILABLE));
            }
        });

        return future;
    }
}
