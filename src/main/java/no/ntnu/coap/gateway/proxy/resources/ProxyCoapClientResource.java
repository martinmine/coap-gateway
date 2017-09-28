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

import no.ntnu.coap.gateway.proxy.CoapTranslator;
import no.ntnu.coap.gateway.proxy.EndPointManagerPool;
import no.ntnu.coap.gateway.proxy.TranslationException;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.EndpointManager;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;


/**
 * Resource that forwards a coap request with the proxy-uri option set to the
 * desired coap server.
 */
public class ProxyCoapClientResource extends ForwardingResource {
    private static final Logger LOGGER = Logger.getLogger(ProxyCoapClientResource.class.getName());

    public ProxyCoapClientResource() {
        this("coapClient");
    }

    public ProxyCoapClientResource(String name) {
        // set the resource hidden
        super(name, true);
        getAttributes().setTitle("Forward the requests to a CoAP server.");
    }

    @Override
    public CompletableFuture<Response> forwardRequest(Request request) {
        final CompletableFuture<Response> future = new CompletableFuture<>();

        //LOGGER.info("ProxyCoAP2CoAP forwards " + request);

        // check the invariant: the request must have the proxy-uri set
        if (!request.getOptions().hasProxyUri()) {
            LOGGER.warning("Proxy-uri option not set.");
            future.complete(new Response(ResponseCode.BAD_OPTION));
            return future;
        }

        // remove the fake uri-path
        // FIXME: HACK // TODO: why? still necessary in new Cf?
        request.getOptions().clearUriPath();

        final EndpointManager endpointManager = EndPointManagerPool.getManager();

        // create a new request to forward to the requested coap server
        Request outgoingRequest = null;
        try {
            // create the new request from the original
            outgoingRequest = CoapTranslator.getRequest(request);

//			// enable response queue for blocking I/O
            // LOL no
//			outgoingRequest.enableResponseQueue(true);

            // get the token from the manager // TODO: necessary?
			//outgoingRequest.setToken(TokenManager.getInstance().acquireToken());


            // receive the response // TODO: don't wait for ever
            outgoingRequest.addMessageObserver(new MessageObserver() {

                @Override
                public void onResponse(Response response) {
                    Response outgoingResponse = CoapTranslator.getResponse(response);
                    future.complete(outgoingResponse);
                    EndPointManagerPool.putClient(endpointManager);
                }

                @Override
                public void onAcknowledgement() {
                }

                @Override
                public void onReject() {
                    LOGGER.warning("Request rejected.");
                    future.complete(new Response(CoapTranslator.STATUS_TIMEOUT));
                    EndPointManagerPool.putClient(endpointManager);
                }

                @Override
                public void onTimeout() {
                    LOGGER.warning("Request timed out.");
                    future.complete(new Response(CoapTranslator.STATUS_TIMEOUT));
                    EndPointManagerPool.putClient(endpointManager);
                }

                @Override
                public void onCancel() {
                    LOGGER.warning("Request canceled.");
                    EndPointManagerPool.putClient(endpointManager);
                    future.complete(new Response(CoapTranslator.STATUS_TIMEOUT));
                }

                @Override
                public void onRetransmission() {
                    LOGGER.info("Trying sending again");
                }
/*
            @Override
            public void onSent() {
            }

            @Override
            public void onReadyToSend() {
            }

            @Override
            public void onSendError(Throwable error) {
                LOGGER.severe("Error while sending: " + error.toString());
            }
            */
            });

            // execute the request
            LOGGER.finer("Sending coap request.");

            if (outgoingRequest.getDestination() == null)
                throw new NullPointerException("Destination is null");
            if (outgoingRequest.getDestinationPort() == 0)
                throw new NullPointerException("Destination port is 0");
            endpointManager.getDefaultEndpoint().sendRequest(outgoingRequest);

            // accept the request sending a separate response to avoid the
            // timeout in the requesting client
            LOGGER.finer("Acknowledge message sent");
        } catch (TranslationException e) {
            LOGGER.warning("Proxy-uri option malformed: " + e.getMessage());
            future.complete(new Response(CoapTranslator.STATUS_FIELD_MALFORMED));
            return future;
        } catch (Exception e) {
            LOGGER.warning("Failed to execute request: " + e.getMessage());
            e.printStackTrace();
            future.complete(new Response(ResponseCode.INTERNAL_SERVER_ERROR));
            return future;
        }


        return future;
    }
}
