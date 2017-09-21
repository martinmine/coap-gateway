package no.ntnu.coap.gateway;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.channel.Channel;
import no.ntnu.coap.gateway.resources.*;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Main entry class for the HTTP server.
 */
public class App {
    public static void main(String[] args) {
        final int portNumber = 8081; // Integer.valueOf(args[0]);
        final URI BASE_URI = URI.create("http://localhost:" + portNumber + "/");

        try {
            final ResourceConfig resourceConfig = new ResourceConfig(TextResource.class);
            final Channel server = NettyHttpContainerProvider.createHttp2Server(BASE_URI, resourceConfig, null);

            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            System.out.println("Ready on " + BASE_URI.toString());

            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
