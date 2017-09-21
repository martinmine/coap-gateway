package no.ntnu.coap.gateway;
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
 ******************************************************************************/

import java.io.IOException;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.CoapExchange;

import org.eclipse.californium.proxy.DirectProxyCoapResolver;
import org.eclipse.californium.proxy.ProxyHttpServer;
import org.eclipse.californium.proxy.resources.ForwardingResource;
import org.eclipse.californium.proxy.resources.ProxyHttpClientResource;

/**
 * Http2CoAP: Insert in browser:
 *     URI: http://localhost:8080/proxy/coap://localhost:PORT/target
 *
 * CoAP2CoAP: Insert in Copper:
 *     URI: coap://localhost:PORT/coap2coap
 *     Proxy: coap://localhost:PORT/targetA
 *
 * CoAP2Http: Insert in Copper:
 *     URI: coap://localhost:PORT/coap2http
 *     Proxy: http://lantersoft.ch/robots.txt
 */
public class ExampleCrossProxy {

    private CoapServer targetServerA;

    public ExampleCrossProxy(final int coapPort, final int httpPort) throws IOException {
        CustomProxyCoapClientResource coap2coap = new CustomProxyCoapClientResource("coap2coap");
        ForwardingResource coap2http = new ProxyHttpClientResource("coap2http");

        // Create CoAP Server on PORT with proxy resources form CoAP to CoAP and HTTP
        targetServerA = new CoapServer(coapPort);
        targetServerA.add(coap2coap);
        targetServerA.add(coap2http);
        targetServerA.add(new TargetResource("target"));
        targetServerA.start();

        ProxyHttpServer httpServer = new ProxyHttpServer(httpPort);
        httpServer.setProxyCoapResolver(new DirectProxyCoapResolver(coap2coap));

        System.out.println("CoAP resource \"target\" available over HTTP at: http://localhost:8080/proxy/coap://localhost:PORT/target");
    }

    /**
     * A simple resource that responds to GET requests with a small response
     * containing the resource's name.
     */
    private static class TargetResource extends CoapResource {

        private int counter = 0;

        public TargetResource(String name) {
            super(name);
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            exchange.respond("Response "+(++counter)+" from resource " + getName());
        }
    }
}
