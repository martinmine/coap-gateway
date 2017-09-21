package no.ntnu.coap.gateway;

import java.io.IOException;

/**
 * Main entry class for the HTTP server.
 */
public class App {
    public static void main(String[] args) throws IOException {
        //System.out.println("Hello world");
        new ExampleCrossProxy();
    }
}
