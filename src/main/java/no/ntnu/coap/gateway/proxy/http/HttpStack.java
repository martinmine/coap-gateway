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
package no.ntnu.coap.gateway.proxy.http;

import no.ntnu.coap.gateway.proxy.http.requesthandlers.BaseRequestHandler;
import no.ntnu.coap.gateway.proxy.http.requesthandlers.ProxyAsyncRequestHandler;
import org.apache.http.*;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.protocol.*;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.*;
import org.eclipse.californium.core.network.config.NetworkConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * Class encapsulating the logic of a http server. The class create a receiver
 * thread that it is always blocked on the listen primitive. For each connection
 * this thread creates a new thread that handles the client/server dialog.
 */
public class HttpStack {

    private static final Logger LOGGER = Logger.getLogger(HttpStack.class.getCanonicalName());

    private static final int SOCKET_TIMEOUT = NetworkConfig.getStandard().getInt(
            NetworkConfig.Keys.HTTP_SERVER_SOCKET_TIMEOUT);
    private static final int SOCKET_BUFFER_SIZE = NetworkConfig.getStandard().getInt(
            NetworkConfig.Keys.HTTP_SERVER_SOCKET_BUFFER_SIZE);
    private static final String SERVER_NAME = "Californium Http Proxy";

    /**
     * Resource associated with the proxying behavior. If a client requests
     * resource indicated by
     * http://proxy-address/PROXY_RESOURCE_NAME/coap-server, the proxying
     * handler will forward the request desired coap server.
     */
    private static final String PROXY_RESOURCE_NAME = "proxy";

    /**
     * The resource associated with the local resources behavior. If a client
     * requests resource indicated by
     * http://proxy-address/LOCAL_RESOURCE_NAME/coap-resource, the proxying
     * handler will forward the request to the local resource requested.
     */
    public static final String LOCAL_RESOURCE_NAME = "local";

    private ListeningIOReactor ioReactor;
    private final IOEventDispatch ioEventDispatch;
    private final int httpPort;

    /**
     * Instantiates a new http stack on the requested port. It creates an http
     * listener thread on the port.
     *
     * @param httpPort the http port
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public HttpStack(int httpPort, RequestHandler requestHandler) throws IOException {
        this.httpPort = httpPort;

        // HTTP parameters for the server
        HttpParams params = new SyncBasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT).setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, SOCKET_BUFFER_SIZE).setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true).setParameter(CoreProtocolPNames.ORIGIN_SERVER, SERVER_NAME);

        // Create HTTP protocol processing chain
        // Use standard server-side protocol interceptors
        HttpRequestInterceptor[] requestInterceptors = new HttpRequestInterceptor[]{new RequestAcceptEncoding()};
        HttpResponseInterceptor[] responseInterceptors = new HttpResponseInterceptor[]{new ResponseContentEncoding(), new ResponseDate(), new ResponseServer(), new ResponseContent(), new ResponseConnControl()};
        HttpProcessor httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);

        // Create request handler registry
        HttpAsyncRequestHandlerRegistry registry = new HttpAsyncRequestHandlerRegistry();

        // register the handler that will reply to the proxy requests
        registry.register("/" + PROXY_RESOURCE_NAME + "/*", new ProxyAsyncRequestHandler(PROXY_RESOURCE_NAME, true, requestHandler));
        // register the handler for the frontend
        registry.register("/" + LOCAL_RESOURCE_NAME + "/*", new ProxyAsyncRequestHandler(LOCAL_RESOURCE_NAME, false, requestHandler));
        // register the default handler for root URIs
        // wrapping a common request handler with an async request handler
        registry.register("*", new BasicAsyncRequestHandler(new BaseRequestHandler()));

        // Create server-side HTTP protocol handler
        HttpAsyncService protocolHandler = new HttpAsyncService(httpProcessor, new DefaultConnectionReuseStrategy(), registry, params);

        // Create HTTP connection factory
        NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory = new DefaultNHttpServerConnectionFactory(params);

        // Create server-side I/O event dispatch
        ioEventDispatch = new DefaultHttpServerIODispatch(protocolHandler, connFactory);

        // Create server-side I/O reactor
        ioReactor = new DefaultListeningIOReactor();
    }

    void start(final boolean isDaemon) {
        // Listen of the given port
        ioReactor.listen(new InetSocketAddress(httpPort));
        LOGGER.info("HttpStack listening on port " + httpPort);

        if (isDaemon) {
            // create the listener thread
            Thread listener = new Thread("HttpStack listener") {
                @Override
                public void run() {
                    LOGGER.info("Submitted http listening to thread 'HttpStack listener'");
                    acceptConnections();
                }
            };

            listener.setDaemon(false);
            listener.start();

            LOGGER.info("HttpStack started");
        } else {
            acceptConnections();
        }
    }

    private void acceptConnections() {

        // Starts the reactor and initiates the dispatch of I/O
        // event notifications to the given IOEventDispatch.
        try {
            LOGGER.info("Waiting for incoming requests");
            ioReactor.execute(ioEventDispatch);
        } catch (IOException e) {
            LOGGER.severe("I/O Exception in HttpStack: " + e.getMessage());
        }

        LOGGER.info("Shutdown HttpStack");
    }
}
