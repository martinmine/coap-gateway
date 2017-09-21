package no.ntnu.coap.gateway;

import java.io.IOException;

/**
 * Main entry class for the HTTP server.
 */
public class App {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Arguments must be [CoAP port] [HTTP port]");
            return;
        }

        int coapPort;
        int httpPort;

        try {
            coapPort = Integer.parseInt(args[0]);
            httpPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Port numbers must be integers");
            return;
        }

        new ExampleCrossProxy(coapPort, httpPort);
    }
}
